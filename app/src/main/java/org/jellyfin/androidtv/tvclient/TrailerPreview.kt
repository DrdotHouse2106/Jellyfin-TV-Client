package org.jellyfin.androidtv.tvclient

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
	val soundPreference = booleanPreference("pref_tvclient_trailer_sound", false)

	/** Wartezeit auf einem Titel, bevor der Trailer startet. */
	val delay = 3.seconds

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
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	var visible by remember(url) { mutableStateOf(false) }

	val player = remember(url) {
		ExoPlayer.Builder(context).build().apply {
			setMediaItem(MediaItem.fromUri(url))
			volume = if (withSound) 1f else 0f
			repeatMode = Player.REPEAT_MODE_OFF
			playWhenReady = true
			prepare()
		}
	}

	DisposableEffect(player, lifecycleOwner) {
		val listener = object : Player.Listener {
			override fun onRenderedFirstFrame() {
				visible = true
			}

			override fun onPlaybackStateChanged(playbackState: Int) {
				// Am Ende wieder das Bild zeigen
				if (playbackState == Player.STATE_ENDED) visible = false
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
			lifecycleOwner.lifecycle.removeObserver(observer)
			player.removeListener(listener)
			player.release()
		}
	}

	AndroidView(
		modifier = modifier.alpha(if (visible) 1f else 0f),
		factory = { viewContext ->
			PlayerView(viewContext).apply {
				useController = false
				resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
				this.player = player
			}
		},
	)
}
