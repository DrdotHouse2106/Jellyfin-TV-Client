package org.jellyfin.androidtv.tvclient

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.jellyfin.androidtv.R
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Trailer in einem kleinen Fenster in der App statt in einer anderen App.
 * Lokale Trailer (auf dem Server) laufen über ExoPlayer, YouTube-Trailer über den offiziellen
 * YouTube-IFrame-Player. „Zurück“ schließt das Fenster. Lässt sich ein YouTube-Video nicht einbetten,
 * öffnet sich die YouTube-App direkt.
 */
object TrailerWindow {
	private const val READY_TIMEOUT_MS = 12_000L

	/** YouTube-ID aus einer Trailer-Adresse (watch?v=, youtu.be/, embed/). */
	fun youtubeKey(url: String?): String? {
		val uri = runCatching { Uri.parse(url ?: return null) }.getOrNull() ?: return null
		val host = uri.host.orEmpty()
		val key = when {
			host.endsWith("youtube.com") && uri.getQueryParameter("v") != null -> uri.getQueryParameter("v")
			host.endsWith("youtube.com") && uri.pathSegments.firstOrNull() in setOf("embed", "shorts") -> uri.pathSegments.getOrNull(1)
			host == "youtu.be" -> uri.pathSegments.firstOrNull()
			else -> null
		}
		return key?.takeIf { it.matches(YOUTUBE_KEY) }
	}

	/** Gültige YouTube-Video-Id (11 Zeichen). Alles andere kommt nie ins Player-HTML. */
	val YOUTUBE_KEY = Regex("[A-Za-z0-9_-]{11}")

	private fun dialog(context: Context): Pair<Dialog, FrameLayout> {
		val dialog = Dialog(context)
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
		val metrics = context.resources.displayMetrics
		val width = (metrics.widthPixels * 0.5f).toInt()
		val container = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
		dialog.setContentView(container, ViewGroup.LayoutParams(width, width * 9 / 16))
		dialog.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.BLACK))
			setLayout(width, width * 9 / 16)
			setGravity(Gravity.CENTER)
			setDimAmount(0.7f)
		}
		return dialog to container
	}

	/** Lokalen Trailer (Stream-Adresse vom Jellyfin-Server) im Fenster abspielen. */
	@OptIn(UnstableApi::class)
	fun showLocal(context: Context, url: String) {
		val (dialog, container) = dialog(context)
		val player = ServerAuth.player(context, url).apply {
			setMediaItem(MediaItem.fromUri(url))
			// Selbst gestartet: volle Lautstärke, unabhängig von Ton und Absenkung der Vorschau
			volume = 1f
			playWhenReady = true
			prepare()
			addListener(object : Player.Listener {
				override fun onPlaybackStateChanged(playbackState: Int) {
					if (playbackState == Player.STATE_ENDED) dialog.dismiss()
				}
			})
		}
		container.addView(PlayerView(context).apply {
			useController = false
			resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
			this.player = player
		}, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		dialog.setOnDismissListener { player.release() }
		dialog.show()
	}

	/** YouTube-Trailer im Fenster abspielen; ohne passende WebView direkt in der YouTube-App. */
	@SuppressLint("SetJavaScriptEnabled")
	fun showYoutube(context: Context, key: String) {
		// Sehr alte WebViews (z. B. Fire TV Stick 1. Generation) können den Player nicht
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return openApp(context, key)

		val (dialog, container) = dialog(context)
		val handler = Handler(Looper.getMainLooper())
		var ready = false
		val webView = try {
			WebView(context)
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "WebView nicht verfügbar")
			return openApp(context, key)
		}

		fun fallback() {
			if (!dialog.isShowing) return
			dialog.dismiss()
			openApp(context, key)
		}

		webView.apply {
			setBackgroundColor(Color.BLACK)
			settings.javaScriptEnabled = true
			settings.domStorageEnabled = true
			settings.mediaPlaybackRequiresUserGesture = false
			webChromeClient = WebChromeClient()
			isFocusable = false
			addJavascriptInterface(object {
				@JavascriptInterface
				fun ready() = handler.post { ready = true }

				@JavascriptInterface
				fun ended() = handler.post { dialog.dismiss() }

				// 2 ungültig, 5 HTML5, 100 nicht gefunden, 101/150/152/153 nicht einbettbar
				@JavascriptInterface
				fun error(code: Int) = handler.post {
					Timber.w("YouTube-Player meldet Fehler %d", code)
					fallback()
				}
			}, "Quallenkino")
		}
		container.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

		val html = youtubeHtml(key, withSound = true, volume = 1f)
		webView.loadDataWithBaseURL("$ORIGIN/", html, "text/html", "UTF-8", null)

		// Kommt der Player nicht in Gang (z. B. ohne Internet), die YouTube-App versuchen
		handler.postDelayed({ if (!ready) fallback() }, READY_TIMEOUT_MS)

		dialog.setOnDismissListener {
			handler.removeCallbacksAndMessages(null)
			webView.loadUrl("about:blank")
			webView.destroy()
		}
		dialog.show()
	}

	const val ORIGIN = "https://de.drdothouse.jellyfintv"

	/**
	 * Seite mit dem offiziellen YouTube-IFrame-Player. Meldet über das JavaScript-Objekt „Quallenkino“
	 * ready(), ended() und error(code). Der Player wird nicht verdeckt oder beschnitten.
	 */
	fun youtubeHtml(key: String, withSound: Boolean, volume: Float = TrailerPreview.volume): String {
		if (!key.matches(YOUTUBE_KEY)) return "<!DOCTYPE html><html><body style=\"background:#000\"></body></html>"
		val percent = (volume.coerceIn(0f, 1f) * 100).toInt()
		val mute = if (withSound) "false" else "true"
		return """
			<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
			<style>html,body,#player{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}</style></head>
			<body><div id="player"></div>
			<script src="https://www.youtube.com/iframe_api"></script>
			<script>
			function onYouTubeIframeAPIReady() {
				new YT.Player('player', {
					width: '100%', height: '100%', videoId: '$key',
					playerVars: { autoplay: 1, controls: 0, rel: 0, playsinline: 1, iv_load_policy: 3, origin: '$ORIGIN' },
					events: {
						onReady: function (e) { Quallenkino.ready(); e.target.setVolume($percent); if ($mute) e.target.mute(); else e.target.unMute(); e.target.playVideo(); },
						onStateChange: function (e) { if (e.data === 0) Quallenkino.ended(); },
						onError: function (e) { Quallenkino.error(e.data); }
					}
				});
			}
			</script></body></html>
		""".trimIndent()
	}

	private fun openApp(context: Context, key: String) {
		try {
			context.startActivity(JellyfinLink.youtubeIntent(context, key))
		} catch (_: ActivityNotFoundException) {
			Toast.makeText(context, R.string.no_player_message, Toast.LENGTH_LONG).show()
		}
	}
}
