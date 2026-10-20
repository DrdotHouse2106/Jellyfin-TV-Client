package org.jellyfin.androidtv

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.di.KoinInitializer
import org.jellyfin.androidtv.tvclient.TvClientLocalAddress
import org.jellyfin.androidtv.tvclient.WhoIsWatching

@Suppress("unused")
class SessionInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		// Jellyfin-TV-Client: Heimnetz-Adresse bei Rückkehr in die App und Netzwechsel neu prüfen
		TvClientLocalAddress.start(context)

		// Jellyfin-TV-Client: „Wer schaut?“ – beim Start nie automatisch das letzte Profil anmelden
		WhoIsWatching.start()

		// Restore system session (nur aufräumen, die Profilauswahl fragt)
		ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
			koin.get<SessionRepository>().restoreSession(destroyOnly = true)
		}
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
