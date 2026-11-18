package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.koin.java.KoinJavaComponent

/** Jellyfin-TV-Client: Link ganz unten auf der Startseite – „Alle Titel durchstöbern“ (wie webOS und Web). */
object AllTitlesLink

class HomeFragmentAllTitlesRow : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val adapter = MutableObjectAdapter<Any>(LinkPresenter())
		adapter.add(AllTitlesLink)
		rowsAdapter.add(ListRow(HeaderItem(""), adapter))
	}
}

/** OK auf den Link öffnet „Alle Titel“. */
fun onAllTitlesClicked(item: Any?): Boolean {
	if (item !is AllTitlesLink) return false
	KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.allTitles)
	return true
}

private class LinkPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
		val shape = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
		val view = TextView(context).apply {
			text = "🔍  " + context.getString(R.string.tvclient_all_titles_link)
			gravity = Gravity.CENTER
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			setPadding(dp(22), dp(10), dp(22), dp(10))
			background = shape
			isFocusable = true
			isFocusableInTouchMode = true
		}
		fun style(focused: Boolean) {
			shape.setColor(if (focused) Color.WHITE else Color.argb(46, 255, 255, 255))
			view.setTextColor(if (focused) Color.BLACK else Color.WHITE)
		}
		style(false)
		view.setOnFocusChangeListener { _, focused -> style(focused) }
		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) = Unit

	override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
