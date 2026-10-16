package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.data.querying.GetLocalRecommendationsRequest
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Zeile mit den Empfehlungen des Plugins "Local Recommendations".
 * Nach dem Anschauen eines Films oder einer Folge wird sie neu geladen,
 * damit Gesehenes verschwindet.
 */
class HomeFragmentLocalRecommendationsRow(
	private val title: String,
	private val query: GetLocalRecommendationsRequest,
) : HomeFragmentRow, KoinComponent {
	private val userPreferences by inject<UserPreferences>()

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]
		val rowAdapter = ItemRowAdapter(context, query, preferParentThumb, cardPresenter, rowsAdapter)
		rowAdapter.setReRetrieveTriggers(arrayOf(ChangeTriggerType.MoviePlayback, ChangeTriggerType.TvPlayback, ChangeTriggerType.LibraryUpdated))

		val row = ListRow(HeaderItem(title), rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}

private val localRecommendationSections = listOf(
	HomeSectionType.LOCAL_RECS_MOVIES,
	HomeSectionType.LOCAL_RECS_SERIES,
	HomeSectionType.LOCAL_RECS_LIVE_TV,
)

/**
 * Fügt die Empfehlungs-Zeilen automatisch ein, wenn [enabled] gesetzt ist und der Benutzer
 * keine davon selbst in seine Startseitenabschnitte gelegt hat. Sie kommen direkt nach
 * "Weiterschauen" / "Als Nächstes", sonst an den Anfang.
 */
fun withLocalRecommendations(sections: List<HomeSectionType>, enabled: Boolean): List<HomeSectionType> {
	if (!enabled || sections.any { it in localRecommendationSections }) return sections

	val anchor = sections.indexOfLast { it == HomeSectionType.RESUME || it == HomeSectionType.NEXT_UP }
	return buildList {
		addAll(sections.subList(0, anchor + 1))
		addAll(localRecommendationSections)
		addAll(sections.subList(anchor + 1, sections.size))
	}
}
