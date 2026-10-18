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
import coil3.request.allowHardware
import coil3.toBitmap
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
	private class Holder(
		view: View,
		val backdrop: AsyncImageView,
		val shade: View,
		val image: ImageView,
		val name: TextView,
		val border: GradientDrawable,
	) : ViewHolder(view)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		// Neutral dunkel; die Farbe der Welt erscheint nur als Rand der ausgewählten Kachel
		val background = GradientDrawable().apply {
			cornerRadius = dp(8).toFloat()
			setColor(Color.parseColor("#141414"))
		}
		val borderDrawable = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
		val backdrop = AsyncImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
		val shade = View(context)
		val image = ImageView(context).apply {
			scaleType = ImageView.ScaleType.FIT_CENTER
			setPadding(dp(18), dp(14), dp(18), dp(26))
		}
		val name = TextView(context).apply {
			gravity = Gravity.CENTER
			setTextColor(Color.WHITE)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			maxLines = 2
			setShadowLayer(dp(4).toFloat(), 0f, 0f, Color.BLACK)
		}
		val border = View(context).apply {
			this.background = borderDrawable
			visibility = View.INVISIBLE
		}
		val root = FrameLayout(context).apply {
			layoutParams = ViewGroup.LayoutParams(dp(220), dp(124))
			this.background = background
			clipToOutline = true
			isFocusable = true
			isFocusableInTouchMode = true
			addView(backdrop, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(shade, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(name, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			addView(border, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
			setOnFocusChangeListener { view, focused ->
				val scale = if (focused) 1.08f else 1f
				view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
				border.visibility = if (focused) View.VISIBLE else View.INVISIBLE
			}
		}
		return Holder(root, backdrop, shade, image, name, borderDrawable)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as Holder
		val world = item as? Worlds.World ?: return
		val color = world.definition.color.toInt()
		val density = holder.name.resources.displayMetrics.density
		// Rand der ausgewählten Kachel in der Farbe der Welt (sehr dunkle Farben hell, damit man ihn sieht)
		holder.border.setStroke((3 * density).toInt(), accent(color))
		holder.name.text = world.name
		// Hintergrundbild eines Titels der Welt, neutral schwarz abgedunkelt
		val backdropUrl = world.backdropUrl
		holder.backdrop.isVisible = backdropUrl != null
		if (backdropUrl != null) holder.backdrop.load(url = backdropUrl, aspectRatio = 16.0 / 9.0)
		holder.shade.background = GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			intArrayOf(Color.argb(0x99, 0, 0, 0), Color.argb(0xC8, 0, 0, 0)),
		)
		val url = world.imageUrl
		// Mit Logo steht der Name klein unten (falls das Logo nicht lädt, bleibt die Kachel lesbar)
		holder.name.isVisible = true
		holder.name.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (url == null) 24f else 13f)
		holder.name.gravity = if (url == null) Gravity.CENTER else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
		holder.name.setPadding(0, 0, 0, if (url == null) 0 else (6 * holder.name.resources.displayMetrics.density).toInt())
		holder.image.isVisible = url != null
		holder.image.setImageDrawable(null)
		holder.image.clearColorFilter()
		holder.image.tag = url
		if (url != null) {
			val imageLoader = KoinJavaComponent.get<coil3.ImageLoader>(coil3.ImageLoader::class.java)
			val request = coil3.request.ImageRequest.Builder(holder.image.context)
				.data(url)
				.allowHardware(false)
				.target(onSuccess = { result ->
					if (holder.image.tag != url) return@target
					val bitmap = result.toBitmap()
					// Logos in Originalfarbe; nur fast schwarze werden aufgehellt
					if (LogoTint.isDark(bitmap)) holder.image.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
					holder.image.setImageBitmap(bitmap)
				})
				.build()
			imageLoader.enqueue(request)
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as Holder).image.setImageDrawable(null)
		viewHolder.image.tag = null
		viewHolder.backdrop.setImageDrawable(null)
	}

	/** Akzentfarbe für den Rand: sehr dunkle Weltfarben (z. B. Star Wars) werden weiß. */
	private fun accent(color: Int): Int =
		if (maxOf(Color.red(color), Color.green(color), Color.blue(color)) < 100) Color.WHITE else color
}
