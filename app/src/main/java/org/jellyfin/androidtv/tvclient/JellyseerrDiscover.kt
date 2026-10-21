package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID

/**
 * Jellyfin-TV-Client: „Für dich“ auf der Anfragen-Seite – Titel, die es noch nicht auf dem Server gibt
 * und zu den eigenen Vorlieben passen. Ausgangspunkt sind Titel mit „Gefällt mir“, Favoriten und zuletzt
 * Gesehenes; dazu holt Jellyseerr passende Empfehlungen. Neuere Titel stehen vorne.
 */
object JellyseerrDiscover {
	private const val MAX_SEEDS = 8
	private const val MAX_RESULTS = 60

	/** Ein Ausgangstitel aus der eigenen Bibliothek. */
	data class Seed(val type: String, val tmdbId: Int)

	/**
	 * Reihenfolge (testbar): neuere Titel zuerst; bei gleichem Jahr die, die öfter empfohlen wurden.
	 * Titel aus der Bibliothek und doppelte Treffer fallen weg.
	 */
	fun rank(candidates: List<Jellyseerr.Media>, excluded: Set<String>): List<Jellyseerr.Media> =
		candidates
			.filter { "${it.type}-${it.id}" !in excluded && it.status == Jellyseerr.Status.NONE }
			.groupBy { "${it.type}-${it.id}" }
			.map { (_, list) -> list.first() to list.size }
			.sortedWith(
				compareByDescending<Pair<Jellyseerr.Media, Int>> { it.first.year ?: "" }
					.thenByDescending { it.second }
					.thenByDescending { it.first.date ?: "" }
			)
			.map { it.first }
			.take(MAX_RESULTS)

	suspend fun forYou(api: ApiClient, userId: UUID?): List<Jellyseerr.Media> = coroutineScope {
		val seeds = seeds(api, userId)
		if (seeds.isEmpty()) return@coroutineScope emptyList()

		val candidates = seeds.map { seed ->
			async { runCatching { Jellyseerr.recommendations(seed.type, seed.tmdbId) }.getOrDefault(emptyList()) }
		}.awaitAll().flatten()

		// Was schon in der Bibliothek ist, meldet Jellyseerr selbst (mediaInfo → Status) – die ganze
		// Bibliothek muss dafür nicht mehr geladen werden
		rank(candidates, seeds.map { "${it.type}-${it.tmdbId}" }.toSet())
	}

	private fun BaseItemDto.tmdbId(): Int? =
		providerIds?.entries?.firstOrNull { it.key.equals("Tmdb", true) }?.value?.toIntOrNull()

	private fun BaseItemDto.seed(): Seed? = tmdbId()?.let { Seed(if (type == BaseItemKind.SERIES) "tv" else "movie", it) }

	/** Ausgangstitel: „Gefällt mir“/Favoriten zuerst, dann zuletzt gesehene Filme und Serien. */
	private suspend fun seeds(api: ApiClient, userId: UUID?): List<Seed> = withContext(Dispatchers.IO) {
		suspend fun query(filters: Set<ItemFilter>?, types: Set<BaseItemKind>, sort: ItemSortBy, limit: Int) = runCatching {
			api.itemsApi.getItems(
				GetItemsRequest(
					userId = userId,
					recursive = true,
					includeItemTypes = types,
					filters = filters,
					sortBy = setOf(sort),
					sortOrder = setOf(SortOrder.DESCENDING),
					fields = setOf(ItemFields.PROVIDER_IDS),
					limit = limit,
					enableImages = false,
					enableTotalRecordCount = false,
				)
			).content.items
		}.getOrDefault(emptyList())

		val loved = query(setOf(ItemFilter.IS_FAVORITE_OR_LIKES), setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES), ItemSortBy.DATE_CREATED, 10)
			.filter { it.userData?.likes != false }
		val movies = query(setOf(ItemFilter.IS_PLAYED), setOf(BaseItemKind.MOVIE), ItemSortBy.DATE_PLAYED, 6)
			.filter { it.userData?.likes != false }
		val episodes = query(setOf(ItemFilter.IS_PLAYED), setOf(BaseItemKind.EPISODE), ItemSortBy.DATE_PLAYED, 40)
		val seriesIds = episodes.mapNotNull { it.seriesId }.distinct().take(6)
		val series = if (seriesIds.isEmpty()) emptyList() else runCatching {
			api.itemsApi.getItems(
				GetItemsRequest(userId = userId, ids = seriesIds, fields = setOf(ItemFields.PROVIDER_IDS), enableImages = false)
			).content.items.filter { it.userData?.likes != false }
		}.getOrDefault(emptyList())

		// Abwechselnd Filme und Serien aus dem Verlauf, Lieblingstitel vorne
		val recent = movies.zip(series).flatMap { listOf(it.first, it.second) } +
			movies.drop(series.size) + series.drop(movies.size)
		(loved + recent).mapNotNull { it.seed() }.distinct().take(MAX_SEEDS)
	}
}
