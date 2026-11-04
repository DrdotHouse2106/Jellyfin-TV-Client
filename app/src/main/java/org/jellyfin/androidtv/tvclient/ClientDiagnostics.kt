package org.jellyfin.androidtv.tvclient

import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.redactSecrets
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * Jellyfin-TV-Client: kurze Fehlermeldungen ins Client-Log des Servers (Dashboard → Protokolle), damit sich
 * Probleme am Fernseher (Player-Hänger, abgebrochene Trailer) auf dem Server nachvollziehen lassen.
 * Enthält nur Gerät, App-Version und technische Angaben: Zugangsdaten werden geschwärzt, Titel in „…“
 * ersetzt. Abschaltbar unter Einstellungen → Erweitert → Absturzberichte („Fehlerhinweise an den Server senden“).
 */
object ClientDiagnostics {
	/** Titel in Anführungszeichen („Tatort“, "Film") durch „…“ ersetzen – sie sagen etwas über das Sehverhalten. */
	fun redact(text: String): String = redactSecrets(text)
		.replace(Regex("„[^“\\n]*“"), "„…“")
		.replace(Regex("\"[^\"\\n]*\""), "\"…\"")
		.take(MAX_LENGTH)

	private const val MAX_LENGTH = 2_000

	fun report(text: String) {
		Timber.w(text)
		val enabled = runCatching {
			KoinJavaComponent.get<TelemetryPreferences>(TelemetryPreferences::class.java)[TelemetryPreferences.diagnosticsEnabled]
		}.getOrDefault(false)
		if (!enabled) return
		val api = runCatching { KoinJavaComponent.get<ApiClient>(ApiClient::class.java) }.getOrNull() ?: return
		if (api.accessToken == null) return
		val body = "Quallenkino ${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE})\n${redact(text)}\n"
		runCatching {
			ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
				runCatching { api.clientLogApi.logFile(body) }.onFailure { Timber.w(it, "Diagnose nicht übertragen") }
			}
		}
	}
}
