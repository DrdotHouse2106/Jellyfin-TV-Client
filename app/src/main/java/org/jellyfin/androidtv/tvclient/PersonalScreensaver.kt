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
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Bildschirmschoner mit Titeln, die zum Benutzer passen – persönliche
 * Empfehlungen (Plugin „Local Recommendations“) und angefangene Filme statt Zufall.
 */
object PersonalScreensaver {
	val enabledPreference = booleanPreference("pref_tvclient_personal_screensaver", true)

	private const val RECOMMENDATIONS_PER_TYPE = 30
	private const val RESUME_LIMIT = 10

	/** Persönliche Titel in zufälliger Reihenfolge; leer, wenn es keine gibt. */
	suspend fun items(api: ApiClient): List<BaseItemDto> = withContext(Dispatchers.IO) {
		val recommendedIds = listOf(LocalRecommendationsType.MOVIES, LocalRecommendationsType.SERIES).flatMap { type ->
			runCatching {
				val request = GetLocalRecommendationsRequest(type, limit = RECOMMENDATIONS_PER_TYPE)
				api.get<BaseItemDtoQueryResult>(request.path, queryParameters = request.queryParameters).content.items
			}.getOrDefault(emptyList()).map { it.id }
		}

		val recommended = if (recommendedIds.isEmpty()) emptyList() else runCatching {
			api.itemsApi.getItems(GetItemsRequest(ids = recommendedIds)).content.items
		}.onFailure { Timber.w(it, "Empfehlungen für den Bildschirmschoner nicht verfügbar") }.getOrDefault(emptyList())

		// Angefangene Filme (Folgen haben kein eigenes Hintergrundbild)
		val resume = runCatching {
			api.itemsApi.getResumeItems(
				GetResumeItemsRequest(
					limit = RESUME_LIMIT,
					mediaTypes = setOf(MediaType.VIDEO),
					includeItemTypes = setOf(BaseItemKind.MOVIE),
				)
			).content.items
		}.getOrDefault(emptyList())

		(recommended + resume)
			.filter { !it.backdropImageTags.isNullOrEmpty() }
			.distinctBy { it.id }
			.shuffled()
	}
}
