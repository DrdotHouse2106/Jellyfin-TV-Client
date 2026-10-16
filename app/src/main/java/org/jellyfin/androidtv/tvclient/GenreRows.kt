package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.home.HomeFragmentBrowseRowDefRow
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID

/**
 * Jellyfin-TV-Client: Genre-Zeilen (z. B. „Action“, „Komödie“, „Anime“) mit zufälligen Filmen und
 * Serien – damit man wie bei Netflix lange weiterscrollen kann. Bei jedem Öffnen neu gemischt.
 */
object GenreRows {
	private const val MAX_ROWS = 12
	private const val ITEMS_PER_ROW = 30
	private val types = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)

	suspend fun loadRows(api: ApiClient, userId: UUID): List<HomeFragmentRow> {
		val genres = try {
			withContext(Dispatchers.IO) {
				api.genresApi.getGenres(userId = userId, includeItemTypes = types, enableTotalRecordCount = false).content.items
			}
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "Genres konnten nicht geladen werden")
			return emptyList()
		}

		return pick(genres.mapNotNull { it.name?.takeIf(String::isNotBlank) }).map { genre ->
			val query = GetItemsRequest(
				userId = userId,
				genres = setOf(genre),
				includeItemTypes = types,
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = ITEMS_PER_ROW,
				imageTypeLimit = 1,
				fields = ItemRepository.browseFields,
				enableTotalRecordCount = false,
			)
			HomeFragmentBrowseRowDefRow(BrowseRowDef(genre, query, ITEMS_PER_ROW))
		}
	}

	/** Zufällige Auswahl ohne Doppelte (Groß-/Kleinschreibung egal). */
	fun pick(names: List<String>, max: Int = MAX_ROWS): List<String> =
		names.distinctBy { it.lowercase() }.shuffled().take(max)
}
