package org.jellyfin.androidtv.tvclient

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.preference.booleanPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Jellyfin-TV-Client: Trailer-Vorschau im Infobereich (wie bei Netflix). Bleibt man einige Sekunden
 * auf einem Film oder einer Serie, läuft statt des Bildes der Trailer – sofern auf dem Server ein
 * lokaler Trailer liegt (YouTube-Trailer lassen sich nicht einbetten).
 */
object TrailerPreview {
	val enabledPreference = booleanPreference("pref_tvclient_trailer_preview", true)
	// Neuer Schlüssel: Trailer laufen jetzt standardmäßig mit Ton (alter Schlüssel „…_sound“ war aus)
	val soundPreference = booleanPreference("pref_tvclient_trailer_sound_on", true)

	/** Lautstärke von Trailern relativ zur Gerätelautstärke (nicht so laut wie Filme). */
	const val volume = 0.75f

	/** Wartezeit auf einem Titel, bevor der Trailer startet. */
	val delay = 3.seconds

	// Zuletzt gespielter Trailer und Position – damit er auf der Detailseite (bzw. nach „Zurück“ im
	// Banner) an derselben Stelle weiterläuft statt neu zu starten
	private const val RESUME_WINDOW_MS = 2 * 60 * 1000L
	@Volatile private var resumeUrl: String? = null
	@Volatile private var resumePositionMs = 0L
	@Volatile private var resumeAt = 0L

	fun savePosition(url: String, positionMs: Long) {
		resumeUrl = url
		resumePositionMs = positionMs
		resumeAt = System.currentTimeMillis()
	}

	fun clearPosition(url: String) {
		if (resumeUrl == url) resumeUrl = null
	}

	/** Position, an der dieser Trailer eben noch lief (oder null). */
	fun resumePosition(url: String?): Long? =
		if (url != null && url == resumeUrl && resumePositionMs > 0 && System.currentTimeMillis() - resumeAt < RESUME_WINDOW_MS) resumePositionMs else null

	/** Adresse des ersten lokalen Trailers oder null. */
	suspend fun streamUrl(api: ApiClient, item: BaseItemDto): String? {
		if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.SERIES) return null

		return try {
			val trailer = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLocalTrailers(item.id).content.firstOrNull()
			} ?: return null

			api.createUrl(
				pathTemplate = "/Videos/{itemId}/stream",
				pathParameters = mapOf("itemId" to trailer.id),
				queryParameters = mapOf("static" to true, "api_key" to api.accessToken),
			)
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "Trailer für %s nicht verfügbar", item.name)
			null
		}
	}
}

/** Spielt den Trailer ohne Bedienelemente; erst sichtbar, sobald das erste Bild da ist. */
@OptIn(UnstableApi::class)
@Composable
fun TrailerVideo(
	url: String,
	withSound: Boolean,
	modifier: Modifier = Modifier,
	onPlayingChange: (Boolean) -> Unit = {},
	/** Bildschirmschoner zurückhalten, solange der Trailer läuft (nicht im Schoner selbst). */
	holdScreensaver: Boolean = true,
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	var visible by remember(url) { mutableStateOf(false) }

	val player = remember(url) {
		ExoPlayer.Builder(context).build().apply {
			setMediaItem(MediaItem.fromUri(url))
			TrailerPreview.resumePosition(url)?.let { seekTo(it) }
			volume = if (withSound) TrailerPreview.volume else 0f
			repeatMode = Player.REPEAT_MODE_OFF
			playWhenReady = true
			prepare()
		}
	}

	DisposableEffect(player, lifecycleOwner) {
		val listener = object : Player.Listener {
			override fun onRenderedFirstFrame() {
				visible = true
				onPlayingChange(true)
			}

			override fun onPlaybackStateChanged(playbackState: Int) {
				// Am Ende wieder das Bild zeigen
				if (playbackState == Player.STATE_ENDED) {
					TrailerPreview.clearPosition(url)
					visible = false
					onPlayingChange(false)
				}
			}
		}
		val observer = LifecycleEventObserver { _, event ->
			when (event) {
				Lifecycle.Event.ON_PAUSE -> player.pause()
				Lifecycle.Event.ON_RESUME -> if (player.playbackState != Player.STATE_ENDED) player.play()
				else -> Unit
			}
		}
		player.addListener(listener)
		lifecycleOwner.lifecycle.addObserver(observer)

		onDispose {
			if (player.playbackState != Player.STATE_ENDED && player.currentPosition > 0) TrailerPreview.savePosition(url, player.currentPosition)
			onPlayingChange(false)
			lifecycleOwner.lifecycle.removeObserver(observer)
			player.removeListener(listener)
			player.release()
		}
	}

	// Jellyfin-TV-Client: kein Bildschirmschoner über einem laufenden Trailer; erscheint er doch, anhalten
	var playing by remember(url) { mutableStateOf(false) }
	val screensaverVisible by rememberScreensaverHold(holdScreensaver && playing)
	DisposableEffect(player) {
		val listener = object : Player.Listener {
			override fun onIsPlayingChanged(isPlaying: Boolean) {
				if (isPlaying) playing = true
				else if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) playing = false
			}
		}
		player.addListener(listener)
		onDispose { player.removeListener(listener) }
	}
	LaunchedEffect(screensaverVisible) {
		if (holdScreensaver && screensaverVisible && player.isPlaying) player.pause()
	}

	// Position laufend merken (die nächste Seite kann schon starten, bevor diese hier verschwindet)
	LaunchedEffect(player) {
		while (true) {
			delay(500)
			if (player.isPlaying) TrailerPreview.savePosition(url, player.currentPosition)
		}
	}

	// Sanft ein- und ausblenden
	val alpha by androidx.compose.animation.core.animateFloatAsState(
		if (visible) 1f else 0f, androidx.compose.animation.core.tween(600), label = "TrailerAlpha",
	)
	AndroidView(
		modifier = modifier.alpha(alpha),
		factory = { viewContext ->
			// TextureView (aus dem Layout), damit Verläufe und Texte über dem Video liegen können
			(android.view.LayoutInflater.from(viewContext).inflate(org.jellyfin.androidtv.R.layout.tvclient_trailer_player, null) as PlayerView).apply {
				useController = false
				resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
				this.player = player
			}
		},
	)
}
