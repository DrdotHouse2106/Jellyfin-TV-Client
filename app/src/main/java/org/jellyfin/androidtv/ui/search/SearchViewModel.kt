package org.jellyfin.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(
	private val searchRepository: SearchRepository
) : ViewModel() {
	companion object {
		// Jellyfin-TV-Client: schneller reagieren beim Tippen (vorher 600 ms)
		private val debounceDuration = 350.milliseconds
		private const val EARLY_GROUPS = 3

		private val groups = mapOf(
			R.string.lbl_movies to setOf(BaseItemKind.MOVIE),
			R.string.lbl_series to setOf(BaseItemKind.SERIES),
			R.string.lbl_episodes to setOf(BaseItemKind.EPISODE),
			R.string.lbl_videos to setOf(BaseItemKind.VIDEO),
			R.string.lbl_programs to setOf(BaseItemKind.LIVE_TV_PROGRAM),
			R.string.channels to setOf(BaseItemKind.LIVE_TV_CHANNEL),
			R.string.lbl_playlists to setOf(BaseItemKind.PLAYLIST),
			R.string.lbl_artists to setOf(BaseItemKind.MUSIC_ARTIST),
			R.string.lbl_albums to setOf(BaseItemKind.MUSIC_ALBUM),
			R.string.lbl_songs to setOf(BaseItemKind.AUDIO),
			R.string.photo_albums to setOf(BaseItemKind.PHOTO_ALBUM),
			R.string.photos to setOf(BaseItemKind.PHOTO),
			R.string.lbl_collections to setOf(BaseItemKind.BOX_SET),
			R.string.lbl_people to setOf(BaseItemKind.PERSON),
		)
	}

	private var searchJob: Job? = null

	private var previousQuery: String? = null

	private val _searchResultsFlow = MutableStateFlow<Collection<SearchResultGroup>>(emptyList())
	val searchResultsFlow = _searchResultsFlow.asStateFlow()

	fun searchImmediately(query: String) = searchDebounced(query, 0.milliseconds)

	fun searchDebounced(query: String, debounce: Duration = debounceDuration): Boolean {
		val trimmed = query.trim()
		if (trimmed == previousQuery) return false
		previousQuery = trimmed

		searchJob?.cancel()

		if (trimmed.isBlank()) {
			_searchResultsFlow.value = emptyList()
			return true
		}

		searchJob = viewModelScope.launch {
			delay(debounce)

			// Jellyfin-TV-Client: Filme, Serien und Folgen sofort zeigen, sobald sie da sind, statt auf
			// alle 14 Abfragen zu warten; der Rest folgt einmal am Ende (die Zeilen werden dabei neu
			// aufgebaut, deshalb nicht nach jeder einzelnen Gruppe).
			val results = arrayOfNulls<SearchResultGroup>(groups.size)
			var earlyShown = false
			groups.entries.mapIndexed { index, (stringRes, itemKinds) ->
				launch {
					val result = searchRepository.search(trimmed, itemKinds)
					results[index] = SearchResultGroup(stringRes, result.getOrNull().orEmpty())
					if (!earlyShown && (0 until EARLY_GROUPS).all { results[it] != null }) {
						earlyShown = true
						_searchResultsFlow.value = results.take(EARLY_GROUPS).filterNotNull()
					}
				}
			}.joinAll()
			_searchResultsFlow.value = results.filterNotNull()
		}

		return true
	}
}
