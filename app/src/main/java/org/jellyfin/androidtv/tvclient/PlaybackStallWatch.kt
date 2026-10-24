package org.jellyfin.androidtv.tvclient

import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * Jellyfin-TV-Client: erkennt einen hängenden Player – die Wiedergabe läuft laut Player, aber die Position
 * steht (z. B. eingefrorenes Bild nach dem Fortsetzen, wenn der Ton-Passthrough nicht wieder anläuft).
 * Beim ersten Hänger versucht der Player es mit einem Sprung an dieselbe Stelle (setzt Bild und Ton neu auf);
 * jeder Hänger landet im Client-Log des Servers (Dashboard → Protokolle), damit er sich nachvollziehen lässt.
 */
class PlaybackStallWatch(private val onRecover: (positionMs: Long) -> Unit) {
	companion object {
		/** So lange darf die Position bei laufender Wiedergabe stehen, bevor es als Hänger gilt. */
		const val STALL_MS = 8_000L
	}

	private var lastPosition = -1L
	private var lastMoveAt = 0L
	private var recoveries = 0
	private var reported = false

	/** Neue Wiedergabe bzw. nach Pause: von vorn zählen. */
	fun reset() {
		lastPosition = -1L
		lastMoveAt = 0L
		reported = false
	}

	/** Alle 500 ms aus der Fortschrittsschleife, nur während der Player „spielt“. */
	fun check(positionMs: Long, now: Long, details: () -> String) {
		if (positionMs != lastPosition || lastMoveAt == 0L) {
			lastPosition = positionMs
			lastMoveAt = now
			reported = false
			return
		}
		if (reported || now - lastMoveAt < STALL_MS) return
		reported = true
		val action = if (recoveries == 0) "Neuansatz an derselben Stelle" else "kein weiterer Neuansatz"
		val text = "Player hängt: Position steht seit ${(now - lastMoveAt) / 1000} s bei $positionMs ms trotz laufender Wiedergabe – $action. ${details()}"
		Timber.e(text)
		report(text)
		if (recoveries == 0) {
			recoveries++
			lastMoveAt = now
			onRecover(positionMs)
		}
	}

	private fun report(text: String) {
		val api = runCatching { KoinJavaComponent.get<ApiClient>(ApiClient::class.java) }.getOrNull() ?: return
		val body = "Quallenkino ${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE})\n$text\n"
		ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
			runCatching { api.clientLogApi.logFile(body) }.onFailure { Timber.w(it, "Hänger-Meldung nicht übertragen") }
		}
	}
}
