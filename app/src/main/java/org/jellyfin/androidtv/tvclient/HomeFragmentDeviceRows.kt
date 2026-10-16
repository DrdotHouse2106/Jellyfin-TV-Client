package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.card.LegacyImageCardView
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

/**
 * Jellyfin-TV-Client: Zeile „Apps“ mit den installierten Apps des Fernsehers.
 */
class HomeFragmentAppsRow : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val apps = DeviceApps.getApps(context)
		if (apps.isEmpty()) return

		val adapter = ArrayObjectAdapter(DeviceCardPresenter())
		adapter.addAll(0, apps)
		rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.tvclient_row_apps)), adapter))
	}
}

/**
 * Jellyfin-TV-Client: Zeile „Gerät“ mit Abkürzungen in die Einstellungen des Fernsehers.
 */
class HomeFragmentDeviceSettingsRow : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val shortcuts = DeviceApps.getSettingsShortcuts(context)
		if (shortcuts.isEmpty()) return

		val adapter = ArrayObjectAdapter(DeviceCardPresenter())
		adapter.addAll(0, shortcuts)
		rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.tvclient_row_device)), adapter))
	}
}

/**
 * Klick auf eine Karte der Zeilen „Apps“ / „Gerät“. Gibt true zurück, wenn die Karte dazu gehört.
 */
fun onDeviceCardClicked(context: Context, item: Any?): Boolean = when (item) {
	is DeviceApps.App -> {
		DeviceApps.launch(context, item.packageName)
		true
	}

	is DeviceApps.SettingsShortcut -> {
		DeviceApps.openSettings(context, item)
		true
	}

	else -> false
}

private class DeviceCardPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val cardView = LegacyImageCardView(parent.context, true)
		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true

		val typedValue = TypedValue()
		parent.context.theme.resolveAttribute(R.attr.cardViewBackground, typedValue, true)
		@ColorInt val color = typedValue.data
		cardView.setBackgroundColor(color)

		return ViewHolder(cardView)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val cardView = viewHolder.view as LegacyImageCardView
		val context = cardView.context

		when (item) {
			is DeviceApps.App -> {
				val banner = DeviceApps.hasBanner(context, item.packageName)
				cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT, if (banner) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER)
				cardView.mainImageView.load(placeholder = DeviceApps.getImage(context, item.packageName))
				cardView.setTitleText(item.label)
			}

			is DeviceApps.SettingsShortcut -> {
				cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT, ImageView.ScaleType.FIT_CENTER)
				// Symbol mit Rand, damit es nicht die ganze Kachel füllt
				val inset = (CARD_HEIGHT * ICON_INSET * context.resources.displayMetrics.density).toInt()
				val icon = ContextCompat.getDrawable(context, item.icon)?.let { InsetDrawable(it, inset) }
				cardView.mainImageView.load(placeholder = icon)
				cardView.setTitleText(context.getString(item.title))
			}
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit

	companion object {
		private const val CARD_WIDTH = 224
		private const val CARD_HEIGHT = 126
		private const val ICON_INSET = 0.3f
	}
}
