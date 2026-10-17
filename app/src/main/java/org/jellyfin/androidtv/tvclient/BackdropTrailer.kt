package org.jellyfin.androidtv.tvclient

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.preference.UserPreferences
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Jellyfin-TV-Client: Trailer, der auf Detailseiten nach kurzer Zeit automatisch oben rechts läuft –
 * „im Hintergrund, aber präsent“. Nicht fokussierbar; Knöpfe und Texte bleiben unverändert.
 *
 * - Lokaler Trailer (Server): ersetzt das Hintergrundbild oben rechts und läuft weich nach links und
 *   unten in den Hintergrund aus.
 * - YouTube: als sauberes 16:9-Rechteck oben rechts, ohne etwas darüberzulegen (YouTube-Richtlinien);
 *   der Verlauf liegt nur außerhalb des Players. Bei Fehlern verschwindet es einfach.
 */
@Composable
fun BoxScope.BackdropTrailer(
	localUrl: String?,
	youtubeKey: String?,
	background: Color,
	enabled: Boolean = true,
	onUnavailable: () -> Unit = {},
	/** Kennzeichnung unter dem YouTube-Player, z. B. „EN“ oder „OmU“ (nichts liegt über dem Player). */
	label: String? = null,
) {
	val userPreferences = koinInject<UserPreferences>()
	if (!enabled || !userPreferences[TrailerPreview.enabledPreference]) return
	val withSound = remember { userPreferences[TrailerPreview.soundPreference] }

	var started by remember(localUrl, youtubeKey) { mutableStateOf(false) }
	LaunchedEffect(localUrl, youtubeKey) {
		delay(2.seconds)
		started = true
	}
	if (!started) return

	if (localUrl != null) {
		Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.55f).aspectRatio(16f / 9f)) {
			TrailerVideo(url = localUrl, withSound = withSound, modifier = Modifier.fillMaxSize())
			// Weich in den Hintergrund auslaufen
			Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to background, 0.35f to Color.Transparent)))
			Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to background)))
		}
	} else if (youtubeKey != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		YoutubeInline(
			key = youtubeKey,
			withSound = withSound,
			onUnavailable = onUnavailable,
			label = label,
			modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 48.dp).fillMaxWidth(0.4f),
		)
	}
}

/** YouTube-IFrame-Player ohne Bedienung, nicht fokussierbar; blendet sich bei Fehlern/Ende aus. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeInline(key: String, withSound: Boolean, modifier: Modifier, onUnavailable: () -> Unit, label: String?) {
	var visible by remember(key) { mutableStateOf(false) }
	var gone by remember(key) { mutableStateOf(false) }
	val alpha by animateFloatAsState(if (visible && !gone) 1f else 0f, tween(600), label = "YoutubeInlineAlpha")
	if (gone) return

	val handler = remember { Handler(Looper.getMainLooper()) }
	val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
	DisposableEffect(key, lifecycleOwner) {
		// Kommt der Player nicht in Gang, einfach ausblenden (der Nutzer hat nichts gedrückt)
		handler.postDelayed({ if (!visible) { gone = true; onUnavailable() } }, 12_000)
		// Seite verlassen oder App im Hintergrund: Vorschau beenden (sonst läuft der Ton weiter)
		val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
			if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) gone = true
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
			handler.removeCallbacksAndMessages(null)
		}
	}

	androidx.compose.foundation.layout.Column(modifier.alpha(alpha), horizontalAlignment = Alignment.End) {
	AndroidView(
		modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
		factory = { context ->
			WebView(context).apply {
				layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				isFocusable = false
				isFocusableInTouchMode = false
				descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
				setBackgroundColor(android.graphics.Color.BLACK)
				settings.javaScriptEnabled = true
				settings.domStorageEnabled = true
				settings.mediaPlaybackRequiresUserGesture = false
				webChromeClient = WebChromeClient()
				addJavascriptInterface(object {
					@JavascriptInterface
					fun ready() = handler.post { visible = true }

					@JavascriptInterface
					fun ended() = handler.post { gone = true }

					@JavascriptInterface
					fun error(code: Int) = handler.post {
						Timber.i("YouTube-Vorschau nicht möglich (%d)", code)
						gone = true
						onUnavailable()
					}
				}, "Quallenkino")
				loadDataWithBaseURL("${TrailerWindow.ORIGIN}/", TrailerWindow.youtubeHtml(key, withSound), "text/html", "UTF-8", null)
			}
		},
		onRelease = { webView ->
			webView.loadUrl("about:blank")
			webView.destroy()
		},
	)
	// Sprache außerhalb des Players kennzeichnen
	if (label != null) {
		org.jellyfin.androidtv.ui.base.Text(
			label,
			fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
			color = Color.White,
			modifier = Modifier
				.padding(top = 6.dp)
				.background(Color(0xFF404040), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
				.padding(horizontal = 6.dp, vertical = 2.dp),
		)
	}
	}
}

/**
 * Jellyfin-TV-Client: Hintergrund-Trailer für die Jellyfin-Detailseite (FullDetailsFragment, Java).
 * [attach] legt eine Compose-Ebene hinter die Zeilen; mit [Holder.item] wird der Titel gesetzt.
 */
object DetailsTrailer {
	class Holder {
		val item = kotlinx.coroutines.flow.MutableStateFlow<org.jellyfin.sdk.model.api.BaseItemDto?>(null)
		val enabled = kotlinx.coroutines.flow.MutableStateFlow(true)

		/** Wird aufgerufen, wenn die Vorschau nicht laufen kann (dann den Trailer-Knopf wieder zeigen). */
		var onUnavailable: Runnable? = null

		/** Vorschau stoppen (z. B. beim großen Trailer-Fenster oder beim Abspielen). */
		fun stop() {
			enabled.value = false
		}
	}

	@JvmStatic
	fun attach(root: ViewGroup): Holder {
		val holder = Holder()
		val view = androidx.compose.ui.platform.ComposeView(root.context).apply {
			isFocusable = false
			setContent {
				val item by holder.item.collectAsState()
				val enabled by holder.enabled.collectAsState()
				val api = koinInject<org.jellyfin.sdk.api.client.ApiClient>()
				val current = item ?: return@setContent
				if (current.type != org.jellyfin.sdk.model.api.BaseItemKind.MOVIE && current.type != org.jellyfin.sdk.model.api.BaseItemKind.SERIES) return@setContent

				var localUrl by remember(current.id) { mutableStateOf<String?>(null) }
				var checked by remember(current.id) { mutableStateOf(false) }
				LaunchedEffect(current.id) {
					if ((current.localTrailerCount ?: 0) > 0) localUrl = TrailerPreview.streamUrl(api, current)
					checked = true
				}
				if (!checked) return@setContent
				val key = current.remoteTrailers.orEmpty().firstNotNullOfOrNull { TrailerWindow.youtubeKey(it.url) }

				Box(Modifier.fillMaxSize()) {
					BackdropTrailer(
						localUrl = localUrl,
						youtubeKey = key.takeIf { localUrl == null },
						background = Color.Black.copy(alpha = 0.85f),
						enabled = enabled,
						onUnavailable = { holder.onUnavailable?.run() },
					)
				}
			}
		}
		// Hinter den Zeilen (Index 1: nach der leeren Hintergrund-Ebene)
		root.addView(view, 1.coerceAtMost(root.childCount), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		return holder
	}
}

/** Jellyfin-TV-Client: Kann der Trailer hier automatisch laufen? Dann ist der Knopf „Trailer abspielen“ überflüssig. */
object TrailerAutoplay {
	private fun previewEnabled(): Boolean = runCatching {
		org.koin.java.KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[TrailerPreview.enabledPreference]
	}.getOrDefault(false)

	fun possible(hasLocalTrailer: Boolean, youtubeKey: String?): Boolean =
		previewEnabled() && (hasLocalTrailer || (youtubeKey != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N))

	/** Für die Jellyfin-Detailseite (Java). */
	@JvmStatic
	fun possible(item: org.jellyfin.sdk.model.api.BaseItemDto): Boolean =
		(item.type == org.jellyfin.sdk.model.api.BaseItemKind.MOVIE || item.type == org.jellyfin.sdk.model.api.BaseItemKind.SERIES) &&
			possible((item.localTrailerCount ?: 0) > 0, item.remoteTrailers.orEmpty().firstNotNullOfOrNull { TrailerWindow.youtubeKey(it.url) })
}
