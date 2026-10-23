package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.querying.GetLocalRecommendationsRequest
import org.jellyfin.androidtv.data.querying.LocalRecommendationsType
import org.jellyfin.preference.booleanPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Bildschirmschoner mit Titeln, die zum Benutzer passen – persönliche
 * Empfehlungen (Plugin „Local Recommendations“) statt Zufall. Wie im Web nur Titel, die noch nicht
 * begonnen oder gesehen sind (sonst stand z. B. der eben gestoppte Film im Schoner).
 */
object PersonalScreensaver {
	val enabledPreference = booleanPreference("pref_tvclient_personal_screensaver", true)

	private const val RECOMMENDATIONS_PER_TYPE = 30

	/** Gesehen oder angefangen (testbar) – solche Titel zeigt der Bildschirmschoner nicht. */
	fun isStartedOrPlayed(item: BaseItemDto): Boolean {
		val data = item.userData ?: return false
		return data.played || data.playbackPositionTicks > 0
	}

	/** Persönliche Titel in zufälliger Reihenfolge; leer, wenn es keine gibt. */
	suspend fun items(api: ApiClient): List<BaseItemDto> = withContext(Dispatchers.IO) {
		val recommendedIds = listOf(LocalRecommendationsType.MOVIES, LocalRecommendationsType.SERIES).flatMap { type ->
			runCatching {
				val request = GetLocalRecommendationsRequest(type, limit = RECOMMENDATIONS_PER_TYPE)
				api.get<BaseItemDtoQueryResult>(request.path, queryParameters = request.queryParameters).content.items
			}.getOrDefault(emptyList()).map { it.id }
		}

		val recommended = if (recommendedIds.isEmpty()) emptyList() else runCatching {
			api.itemsApi.getItems(GetItemsRequest(ids = recommendedIds, enableUserData = true)).content.items
		}.onFailure { Timber.w(it, "Empfehlungen für den Bildschirmschoner nicht verfügbar") }.getOrDefault(emptyList())

		recommended
			.filter { !it.backdropImageTags.isNullOrEmpty() && !isStartedOrPlayed(it) }
			.distinctBy { it.id }
			.shuffled()
	}
}
