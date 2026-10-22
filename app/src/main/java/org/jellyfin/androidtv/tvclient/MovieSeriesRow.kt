package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent

/**
 * Jellyfin-TV-Client: Zeile „Filmreihen für dich“ wie im Web – Karte mit Bild und Name der Reihe,
 * Fortschrittsbalken und darunter „1 von 3 gesehen · Weiter mit Teil 2“ bzw. „4 Teile“.
 */
class HomeFragmentMovieSeriesRow(private val title: String, private val series: List<TvClientSections.MovieSeries>) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (series.isEmpty()) return
		val adapter = MutableObjectAdapter<Any>(MovieSeriesPresenter())
		series.forEach(adapter::add)
		rowsAdapter.add(ListRow(HeaderItem(title), adapter))
	}
}

/** OK auf eine Reihen-Karte öffnet die Reihen-Seite. */
fun onMovieSeriesClicked(item: Any?): Boolean {
	if (item !is TvClientSections.MovieSeries) return false
	KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.movieSeries(item.id))
	return true
}

private class MovieSeriesPresenter : Presenter() {
	private class Holder(
		view: View,
		val backdrop: AsyncImageView,
		val name: TextView,
		val progress: View,
		val progressTrack: View,
		val caption: TextView,
	) : ViewHolder(view)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		val background = GradientDrawable().apply {
			cornerRadius = dp(8).toFloat()
			setColor(Color.parseColor("#141414"))
		}
		val borderDrawable = GradientDrawable().apply {
			cornerRadius = dp(8).toFloat()
			setStroke(dp(3), Color.WHITE)
		}
		val backdrop = AsyncImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
		val shade = View(context).apply {
			this.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.argb(0x88, 0, 0, 0), Color.argb(0xC8, 0, 0, 0)))
		}
		val name = TextView(context).apply {
			gravity = Gravity.CENTER
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			maxLines = 3
			setPadding(dp(10), 0, dp(10), 0)
			setShadowLayer(dp(4).toFloat(), 0f, 0f, Color.BLACK)
		}
		val track = View(context).apply { this.background = GradientDrawable().apply { cornerRadius = dp(2).toFloat(); setColor(Color.argb(0x55, 255, 255, 255)) } }
		val progress = View(context).apply { this.background = GradientDrawable().apply { cornerRadius = dp(2).toFloat(); setColor(Color.parseColor("#E50914")) } }
		val border = View(context).apply {
			this.background = borderDrawable
			visibility = View.INVISIBLE
		}
		val card = FrameLayout(context).apply {
			this.background = background
			clipToOutline = true
			addView(backdrop, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(shade, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(name, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(track, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4), Gravity.BOTTOM).apply { setMargins(dp(10), 0, dp(10), dp(8)) })
			addView(progress, FrameLayout.LayoutParams(0, dp(4), Gravity.BOTTOM or Gravity.START).apply { setMargins(dp(10), 0, 0, dp(8)) })
			addView(border, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		}
		val caption = TextView(context).apply {
			setTextColor(Color.parseColor("#B3B3B3"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			maxLines = 2
			setPadding(dp(2), dp(6), dp(2), 0)
		}
		val root = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
			isFocusable = true
			isFocusableInTouchMode = true
			addView(card, LinearLayout.LayoutParams(dp(220), dp(124)))
			addView(caption, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
			setOnFocusChangeListener { _, focused ->
				val scale = if (focused) 1.08f else 1f
				card.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
				border.visibility = if (focused) View.VISIBLE else View.INVISIBLE
				caption.setTextColor(if (focused) Color.WHITE else Color.parseColor("#B3B3B3"))
			}
		}
		return Holder(root, backdrop, name, progress, track, caption)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as Holder
		val series = item as? TvClientSections.MovieSeries ?: return
		val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
		holder.name.text = series.name
		holder.caption.text = series.caption
		// Bild der Reihe, sonst Hintergrund des nächsten Teils
		val url = series.imageUrl(api.baseUrl) ?: series.next?.let { heroBackdropImage(it)?.getUrl(api, maxWidth = 640) }
		if (url != null) holder.backdrop.load(url = url, aspectRatio = 16.0 / 9.0) else holder.backdrop.setImageDrawable(null)
		// Fortschritt: gesehene Teile
		val density = holder.name.resources.displayMetrics.density
		val width = ((220 - 20) * density).toInt()
		val fraction = if (series.total > 0) series.watched.toFloat() / series.total else 0f
		holder.progress.visibility = if (fraction > 0f) View.VISIBLE else View.GONE
		holder.progressTrack.visibility = holder.progress.visibility
		holder.progress.layoutParams = (holder.progress.layoutParams as FrameLayout.LayoutParams).apply { this.width = (width * fraction).toInt() }
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as Holder).backdrop.setImageDrawable(null)
	}
}
