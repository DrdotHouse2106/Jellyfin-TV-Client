package org.jellyfin.androidtv.tvclient

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.UUID

/**
 * Jellyfin-TV-Client: Schauspieler auf einer Detailseite öffnen. Mit Jellyseerr und bekannter TMDB-Id die ganze
 * Filmografie (in der Bibliothek und weitere Titel zum Anfragen), sonst wie bisher die Personenseite des Servers.
 */
object TvClientPersonLink {
	@JvmStatic
	fun open(personId: UUID?) {
		personId ?: return
		val navigation = KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java)
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val tmdb = runCatching { tmdbId(personId) }
				.onFailure { Timber.w(it, "TMDB-Id der Person nicht verfügbar") }
				.getOrNull()
			navigation.navigate(if (tmdb != null) Destinations.jellyseerrPerson(tmdb) else Destinations.itemDetails(personId))
		}
	}

	private suspend fun tmdbId(personId: UUID): Int? = withContext(Dispatchers.IO) {
		val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
		if (Jellyseerr.detect(api) == null) return@withContext null
		val person = api.userLibraryApi.getItem(itemId = personId).content
		person.providerIds?.entries?.firstOrNull { it.key.equals("Tmdb", ignoreCase = true) }?.value?.toIntOrNull()
	}
}
