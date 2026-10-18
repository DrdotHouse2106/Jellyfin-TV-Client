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
import android.widget.TextView
import androidx.core.view.isVisible
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
import org.koin.java.KoinJavaComponent

/** Jellyfin-TV-Client: Reihe „Welten“ auf der Startseite mit großen Kacheln (Logo oder Name). */
class HomeFragmentWorldsRow(private val worlds: List<Worlds.World>) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (worlds.isEmpty()) return
		val adapter = MutableObjectAdapter<Any>(WorldPresenter())
		worlds.forEach(adapter::add)
		rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.tvclient_home_worlds)), adapter))
	}
}

/** OK auf eine Welt-Kachel öffnet die Welt-Seite. */
fun onWorldClicked(item: Any?): Boolean {
	if (item !is Worlds.World) return false
	KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.world(item.key))
	return true
}

private class WorldPresenter : Presenter() {
	private class Holder(view: View, val image: AsyncImageView, val name: TextView, val background: GradientDrawable) : ViewHolder(view)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		val background = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
		val image = AsyncImageView(context).apply {
			scaleType = ImageView.ScaleType.FIT_CENTER
			setPadding(dp(18), dp(14), dp(18), dp(14))
		}
		val name = TextView(context).apply {
			gravity = Gravity.CENTER
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			maxLines = 2
		}
		val border = View(context).apply {
			this.background = GradientDrawable().apply {
				cornerRadius = dp(8).toFloat()
				setStroke(dp(3), Color.WHITE)
			}
			visibility = View.INVISIBLE
		}
		val root = FrameLayout(context).apply {
			layoutParams = ViewGroup.LayoutParams(dp(220), dp(124))
			this.background = background
			clipToOutline = true
			isFocusable = true
			isFocusableInTouchMode = true
			addView(name, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(border, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			setOnFocusChangeListener { view, focused ->
				val scale = if (focused) 1.08f else 1f
				view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
				border.visibility = if (focused) View.VISIBLE else View.INVISIBLE
			}
		}
		return Holder(root, image, name, background)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as Holder
		val world = item as? Worlds.World ?: return
		val color = world.definition.color.toInt()
		holder.background.orientation = GradientDrawable.Orientation.TL_BR
		holder.background.colors = intArrayOf(color, darken(color))
		holder.name.text = world.name
		val url = world.imageUrl
		holder.name.isVisible = url == null
		holder.image.isVisible = url != null
		if (url != null) holder.image.load(url = url, aspectRatio = 16.0 / 9.0)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as Holder).image.setImageDrawable(null)
	}

	private fun darken(color: Int): Int = Color.rgb(
		(Color.red(color) * 0.35f).toInt(),
		(Color.green(color) * 0.35f).toInt(),
		(Color.blue(color) * 0.35f).toInt(),
	)
}
