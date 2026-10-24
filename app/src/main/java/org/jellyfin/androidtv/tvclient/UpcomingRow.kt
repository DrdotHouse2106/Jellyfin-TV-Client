package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
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
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.koin.java.KoinJavaComponent

/**
 * Jellyfin-TV-Client: Zeile „Demnächst“ wie im Web – Bild, Titel und Untertitel (Termin, Staffel/Folge).
 * Folgen öffnen die Serie, Filme zeigen Titel, Jahr und Beschreibung (es wird nichts angefragt).
 */
class HomeFragmentUpcomingRow(private val title: String, private val entries: List<TvClientSections.Upcoming>) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (entries.isEmpty()) return
		val adapter = MutableObjectAdapter<Any>(UpcomingPresenter())
		entries.forEach(adapter::add)
		rowsAdapter.add(ListRow(HeaderItem(title), adapter))
	}
}

/**
 * OK auf einen Eintrag wie im Web: Folge zeigt „SxxEyy · Folgentitel“, Termin, Sender, Beschreibung und den Knopf
 * „Zur Serie“; ein Film zeigt Titel, Jahr, Termin und Beschreibung (es wird nichts angefragt).
 */
fun onUpcomingClicked(context: Context, item: Any?): Boolean {
	if (item !is TvClientSections.Upcoming) return false
	val series = item.series
	if (series != null) {
		val episode = item.episode ?: TvClientSections.Episode()
		val more = episode.more.takeIf { it > 0 }?.let { context.resources.getQuantityString(R.plurals.tvclient_upcoming_more, it, it) }
		TvInfoDialog.show(
			context,
			item.title,
			listOfNotNull(
				item.subtitle?.let { TvInfoDialog.Line(it, TvInfoDialog.Style.REASON) },
				TvInfoDialog.Line(episode.line, TvInfoDialog.Style.STRONG),
				TvInfoDialog.Line(episode.whenText(), TvInfoDialog.Style.MUTED),
				episode.overview?.let { TvInfoDialog.Line(it) },
				more?.let { TvInfoDialog.Line(it, TvInfoDialog.Style.MUTED) },
			),
			listOf(
				TvInfoDialog.Button(context.getString(R.string.tvclient_upcoming_to_series)) {
					KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.itemDetails(series.id))
				},
				TvInfoDialog.Button(context.getString(R.string.tvclient_upcoming_close)),
			),
		)
	} else {
		TvInfoDialog.show(
			context,
			listOfNotNull(item.title, item.year?.toString()).joinToString(" · "),
			listOfNotNull(
				item.subtitle?.let { TvInfoDialog.Line(it, TvInfoDialog.Style.REASON) },
				item.overview?.let { TvInfoDialog.Line(it) },
			),
			listOf(TvInfoDialog.Button(context.getString(R.string.tvclient_upcoming_close))),
		)
	}
	return true
}

private class UpcomingPresenter : Presenter() {
	private class Holder(view: View, val image: AsyncImageView, val placeholder: TextView, val title: TextView, val subtitle: TextView) : ViewHolder(view)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
		val background = GradientDrawable().apply {
			cornerRadius = dp(8).toFloat()
			setColor(Color.parseColor("#141414"))
		}
		val border = View(context).apply {
			this.background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setStroke(dp(3), Color.WHITE) }
			visibility = View.INVISIBLE
		}
		val image = AsyncImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
		val placeholder = TextView(context).apply {
			gravity = android.view.Gravity.CENTER
			setTextColor(Color.parseColor("#B3B3B3"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
			setPadding(dp(10), 0, dp(10), 0)
		}
		val card = FrameLayout(context).apply {
			this.background = background
			clipToOutline = true
			addView(placeholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(border, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		}
		val title = TextView(context).apply {
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			maxLines = 1
			ellipsize = android.text.TextUtils.TruncateAt.END
			setPadding(dp(2), dp(6), dp(2), 0)
		}
		val subtitle = TextView(context).apply {
			setTextColor(Color.parseColor("#B3B3B3"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
			maxLines = 2
			setPadding(dp(2), dp(2), dp(2), 0)
		}
		val root = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = ViewGroup.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
			isFocusable = true
			isFocusableInTouchMode = true
			addView(card, LinearLayout.LayoutParams(dp(220), dp(124)))
			addView(title, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
			addView(subtitle, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
			setOnFocusChangeListener { _, focused ->
				val scale = if (focused) 1.08f else 1f
				card.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
				border.visibility = if (focused) View.VISIBLE else View.INVISIBLE
			}
		}
		return Holder(root, image, placeholder, title, subtitle)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as Holder
		val entry = item as? TvClientSections.Upcoming ?: return
		holder.title.text = entry.title
		holder.subtitle.text = entry.subtitle.orEmpty()
		holder.placeholder.text = entry.title
		// Folge: Bild der Serie; Film: Bild aus Radarr (ohne Anmeldung)
		val url = entry.imageUrl ?: entry.series?.let { series ->
			val api = KoinJavaComponent.get<org.jellyfin.sdk.api.client.ApiClient>(org.jellyfin.sdk.api.client.ApiClient::class.java)
			heroBackdropImage(series)?.getUrl(api, maxWidth = 640)
				?: KoinJavaComponent.get<ImageHelper>(ImageHelper::class.java).getPrimaryImageUrl(series, height = 248)
		}
		if (url != null) holder.image.load(url = url, aspectRatio = 16.0 / 9.0) else holder.image.setImageDrawable(null)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as Holder).image.setImageDrawable(null)
	}
}
