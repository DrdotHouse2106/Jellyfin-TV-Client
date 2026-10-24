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
 * Jellyfin-TV-Client: kurze Fehlermeldungen ins Client-Log des Servers (Dashboard → Protokolle), damit sich
 * Probleme am Fernseher (Player-Hänger, abgebrochene Trailer) auf dem Server nachvollziehen lassen.
 * Enthält nur Gerät, App-Version und technische Angaben – keine Anmeldedaten.
 */
object ClientDiagnostics {
	fun report(text: String) {
		Timber.w(text)
		val api = runCatching { KoinJavaComponent.get<ApiClient>(ApiClient::class.java) }.getOrNull() ?: return
		if (api.accessToken == null) return
		val body = "Quallenkino ${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE})\n$text\n"
		runCatching {
			ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
				runCatching { api.clientLogApi.logFile(body) }.onFailure { Timber.w(it, "Diagnose nicht übertragen") }
			}
		}
	}
}
