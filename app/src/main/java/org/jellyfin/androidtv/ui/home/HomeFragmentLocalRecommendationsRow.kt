package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.querying.ExternalItemsRequest
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
	private val query: ExternalItemsRequest,
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
