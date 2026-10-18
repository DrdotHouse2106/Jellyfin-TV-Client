package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Jellyfin-TV-Client: Zeile „Top 10 bei euch“ – die in letzter Zeit meistgesehenen Filme und Serien
 * auf dem Server, mit großen Zahlen neben den Postern.
 *
 * Gezählt wird, wie viele Benutzer einen Titel (bei Serien: irgendeine Folge) in den letzten
 * [DAYS] Tagen gesehen haben, danach die Anzahl der Wiedergaben. Die Zahlen kommen vom Plugin
 * „Local Recommendations“ (ab 12.6.0, alle Benutzer). Ohne Plugin zählt die App selbst:
 * als Administrator über alle Benutzer, sonst nur das eigene Konto.
 */
object Top10 {
	private const val DAYS = 180L
	private const val PER_USER = 300
	private const val MAX_USERS = 25
	private const val SIZE = 10

	data class Usage(val key: UUID, val userId: UUID, val plays: Int)

	/** Reine Rangfolge (testbar): viele verschiedene Zuschauer zuerst, dann viele Wiedergaben. */
	fun rank(usages: List<Usage>, size: Int = SIZE): List<UUID> = usages
		.groupBy { it.key }
		.map { (key, list) -> Triple(key, list.map { it.userId }.toSet().size, list.sumOf { it.plays }) }
		.sortedWith(compareByDescending<Triple<UUID, Int, Int>> { it.second }.thenByDescending { it.third })
		.take(size)
		.map { it.first }

	/** Rangfolge vom Plugin „Local Recommendations“ (ab 12.6.0) – über alle Benutzer, ohne Admin-Rechte. */
	private suspend fun loadFromPlugin(api: ApiClient): List<UUID>? = runCatching {
		api.get<BaseItemDtoQueryResult>(
			"/LocalRecs/Top10",
			queryParameters = mapOf("type" to "all", "days" to DAYS, "limit" to SIZE),
		).content.items.map { it.id }
	}.onFailure { Timber.i("Top 10 vom Plugin nicht verfügbar: %s", it.message) }.getOrNull()

	suspend fun load(api: ApiClient, currentUser: UserDto): List<BaseItemDto> = withContext(Dispatchers.IO) {
		try {
			loadFromPlugin(api)?.takeIf { it.isNotEmpty() }?.let { ids -> return@withContext itemsInOrder(api, currentUser, ids) }

			val users = if (currentUser.policy?.isAdministrator == true) {
				runCatching { api.userApi.getUsers().content }.getOrNull()?.take(MAX_USERS) ?: listOf(currentUser)
			} else {
				listOf(currentUser)
			}
			val since = LocalDateTime.now().minusDays(DAYS)

			val usages = coroutineScope {
				users.map { user ->
					async {
						runCatching {
							api.itemsApi.getItems(
								GetItemsRequest(
									userId = user.id,
									recursive = true,
									includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
									filters = setOf(ItemFilter.IS_PLAYED),
									sortBy = setOf(ItemSortBy.DATE_PLAYED),
									sortOrder = setOf(SortOrder.DESCENDING),
									limit = PER_USER,
									enableUserData = true,
									enableImages = false,
									enableTotalRecordCount = false,
								)
							).content.items
						}.getOrDefault(emptyList()).mapNotNull { item ->
							val lastPlayed = item.userData?.lastPlayedDate ?: return@mapNotNull null
							if (lastPlayed.isBefore(since)) return@mapNotNull null
							val key = if (item.type == BaseItemKind.EPISODE) item.seriesId ?: return@mapNotNull null else item.id
							Usage(key, user.id, (item.userData?.playCount ?: 1).coerceAtLeast(1))
						}
					}
				}.awaitAll().flatten()
			}

			itemsInOrder(api, currentUser, rank(usages))
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "Top 10 konnte nicht geladen werden")
			emptyList()
		}
	}
}

/** Titel mit allen Feldern für Infobereich und Karten laden, Reihenfolge beibehalten. */
private suspend fun itemsInOrder(api: ApiClient, currentUser: UserDto, ids: List<UUID>): List<BaseItemDto> {
	if (ids.isEmpty()) return emptyList()
	val byId = api.itemsApi.getItems(
		GetItemsRequest(
			userId = currentUser.id,
			ids = ids,
			fields = ItemRepository.browseFields,
			enableUserData = true,
			enableTotalRecordCount = false,
		)
	).content.items.associateBy { it.id }
	return ids.mapNotNull { byId[it] }
}

class HomeFragmentTop10Row(private val items: List<BaseItemDto>) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		if (items.size < 3) return
		val adapter = MutableObjectAdapter<Any>(Top10Presenter())
		items.forEachIndexed { index, item -> adapter.add(Top10RowItem(item, index + 1)) }
		rowsAdapter.add(ListRow(HeaderItem(context.getString(R.string.tvclient_home_top10)), adapter))
	}
}

/** Titel mit seinem Platz in der Top 10. */
class Top10RowItem(item: BaseItemDto, val rank: Int) : BaseItemDtoBaseRowItem(item)

private class Top10Presenter : Presenter() {
	private class Holder(view: FrameLayout, val number: AppCompatTextView, val poster: AsyncImageView) : ViewHolder(view)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

		// Große dunkle Zahl mit hellem Schein als Umriss, links neben dem Plakat (nur „10“ ragt etwas darunter)
		val number = AppCompatTextView(context).apply {
			setTextColor(Color.parseColor("#2B2B2B"))
			setShadowLayer(dp(3).toFloat(), 0f, 0f, Color.parseColor("#D0D0D0"))
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 96f)
			typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
			includeFontPadding = false
			gravity = Gravity.BOTTOM or Gravity.START
		}
		val poster = AsyncImageView(context).apply {
			scaleType = ImageView.ScaleType.CENTER_CROP
			clipToOutline = true
			background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#262626")) }
		}
		val root = FrameLayout(context).apply {
			layoutParams = ViewGroup.LayoutParams(dp(180), dp(150))
			isFocusable = true
			isFocusableInTouchMode = true
			clipChildren = false
			addView(number, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START or Gravity.BOTTOM))
			addView(poster, FrameLayout.LayoutParams(dp(100), dp(150), Gravity.END))
			val border = android.view.View(context).apply {
				background = GradientDrawable().apply {
					cornerRadius = dp(4).toFloat()
					setStroke(dp(3), Color.WHITE)
				}
				visibility = android.view.View.INVISIBLE
			}
			addView(border, FrameLayout.LayoutParams(dp(100), dp(150), Gravity.END))
			setOnFocusChangeListener { view, focused ->
				val scale = if (focused) 1.08f else 1f
				view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
				border.visibility = if (focused) android.view.View.VISIBLE else android.view.View.INVISIBLE
			}
		}
		return Holder(root, number, poster)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val holder = viewHolder as Holder
		val rowItem = item as? Top10RowItem ?: return
		val baseItem = rowItem.baseItem ?: return
		holder.number.text = rowItem.rank.toString()
		val imageHelper = KoinJavaComponent.get<ImageHelper>(ImageHelper::class.java)
		holder.poster.load(url = imageHelper.getPrimaryImageUrl(baseItem, height = 300), aspectRatio = 2.0 / 3.0)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder as Holder).poster.setImageDrawable(null)
	}
}
