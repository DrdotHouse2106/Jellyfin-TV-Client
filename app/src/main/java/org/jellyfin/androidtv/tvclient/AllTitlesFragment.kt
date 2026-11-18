package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * Jellyfin-TV-Client: „Alle Titel“ (Link ganz unten auf der Startseite, wie in der webOS-App und im Web):
 * alle Filme und Serien als Banner-Karten, oben der große Infobereich zum ausgewählten Titel – so blättert man
 * durch die Banner. Filter Filme/Serien, Sortierung, lädt beim Weiterscrollen nach.
 */
class AllTitlesFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme { AllTitlesScreen() }
	}
}

object AllTitles {
	enum class Type(val kinds: Set<BaseItemKind>) {
		ALL(setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)),
		MOVIES(setOf(BaseItemKind.MOVIE)),
		SERIES(setOf(BaseItemKind.SERIES)),
	}

	enum class Sort(val by: ItemSortBy) {
		NAME(ItemSortBy.SORT_NAME),
		ADDED(ItemSortBy.DATE_CREATED),
		YEAR(ItemSortBy.PRODUCTION_YEAR),
		RATING(ItemSortBy.COMMUNITY_RATING),
	}

	const val PER_ROW = 5
	const val PAGE = 60

	/** Reihen zu je [PER_ROW] Titeln (testbar). */
	fun <T> rows(items: List<T>): List<List<T>> = items.chunked(PER_ROW)

	suspend fun load(api: ApiClient, userId: java.util.UUID, type: Type, sort: Sort, start: Int): Pair<List<BaseItemDto>, Int> = withContext(Dispatchers.IO) {
		val result = api.itemsApi.getItems(
			GetItemsRequest(
				userId = userId,
				includeItemTypes = type.kinds,
				recursive = true,
				sortBy = listOf(sort.by, ItemSortBy.SORT_NAME),
				sortOrder = listOf(if (sort == Sort.NAME) SortOrder.ASCENDING else SortOrder.DESCENDING),
				startIndex = start,
				limit = PAGE,
				imageTypeLimit = 1,
				fields = ItemRepository.browseFields,
			)
		).content
		result.items to result.totalRecordCount
	}
}

@Composable
private fun AllTitlesScreen() {
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val navigationRepository = koinInject<NavigationRepository>()
	val scope = rememberCoroutineScope()
	var type by remember { mutableStateOf(AllTitles.Type.ALL) }
	var sort by remember { mutableStateOf(AllTitles.Sort.NAME) }
	var items by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
	var total by remember { mutableStateOf<Int?>(null) }
	var focused by remember { mutableStateOf<BaseItemDto?>(null) }
	var loading by remember { mutableStateOf(false) }
	val firstCard = remember { FocusRequester() }

	suspend fun load(reset: Boolean) {
		val userId = userRepository.currentUser.value?.id ?: return
		loading = true
		val (page, count) = runCatching { AllTitles.load(api, userId, type, sort, if (reset) 0 else items.size) }
			.onFailure { Timber.w(it, "Alle Titel konnten nicht geladen werden") }
			.getOrDefault(emptyList<BaseItemDto>() to 0)
		total = count
		items = if (reset) page else items + page
		loading = false
		if (reset && page.isNotEmpty()) {
			focused = page.first()
			delay(100)
			runCatching { firstCard.requestFocus() }
		}
	}

	LaunchedEffect(type, sort) { load(reset = true) }
	val rows = remember(items) { AllTitles.rows(items) }

	Box(Modifier.fillMaxSize().background(Color.Black)) {
		// Hintergrund des ausgewählten Titels oben rechts (wie Startseite und Welten)
		focused?.let(::heroBackdropImage)?.let { image ->
			Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.7f).fillMaxHeight(0.6f)) {
				coil3.compose.AsyncImage(
					model = image.getUrl(api, maxWidth = 1280),
					contentDescription = null,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
				Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black, 0.45f to Color.Black.copy(alpha = 0.5f), 0.8f to Color.Transparent)))
				Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black)))
			}
		}

		Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 24.dp, top = 0.dp)) {
			Box(Modifier.height(230.dp).fillMaxWidth()) {
				focused?.let { HeroContent(it) }
			}
			Text(
				stringResource(R.string.tvclient_all_titles) + (total?.let { " · " + stringResource(R.string.tvclient_all_titles_count, it) } ?: ""),
				fontSize = 15.sp,
				color = Color(0xFFB8BFCC),
				modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
			)
			Row(Modifier.padding(start = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				FilterButton(stringResource(R.string.tvclient_all_titles_all), type == AllTitles.Type.ALL) { type = AllTitles.Type.ALL }
				FilterButton(stringResource(R.string.tvclient_all_titles_movies), type == AllTitles.Type.MOVIES) { type = AllTitles.Type.MOVIES }
				FilterButton(stringResource(R.string.tvclient_all_titles_series), type == AllTitles.Type.SERIES) { type = AllTitles.Type.SERIES }
				FilterButton(stringResource(R.string.tvclient_all_titles_sort_name), sort == AllTitles.Sort.NAME) { sort = AllTitles.Sort.NAME }
				FilterButton(stringResource(R.string.tvclient_all_titles_sort_added), sort == AllTitles.Sort.ADDED) { sort = AllTitles.Sort.ADDED }
				FilterButton(stringResource(R.string.tvclient_all_titles_sort_year), sort == AllTitles.Sort.YEAR) { sort = AllTitles.Sort.YEAR }
				FilterButton(stringResource(R.string.tvclient_all_titles_sort_rating), sort == AllTitles.Sort.RATING) { sort = AllTitles.Sort.RATING }
			}
			Spacer(Modifier.height(8.dp))
			LazyColumn(Modifier.weight(1f).clipToBounds(), contentPadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = 120.dp)) {
				if (total == null) item { Text(stringResource(R.string.loading), color = Color(0xFFB3B3B3)) }
				itemsIndexed(rows, key = { index, row -> "$index-${row.first().id}" }) { rowIndex, row ->
					Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						row.forEachIndexed { column, item ->
							var modifier = Modifier.width(200.dp)
							if (rowIndex == 0 && column == 0) modifier = modifier.focusRequester(firstCard)
							BannerCard(item, modifier, onFocus = {
								focused = item
								// Kurz vor dem Ende die nächste Seite laden
								if (!loading && (total ?: 0) > items.size && rowIndex >= rows.size - 3) scope.launch { load(reset = false) }
							}) { navigationRepository.navigate(Destinations.itemDetails(item.id)) }
						}
					}
				}
			}
		}
	}
}

@Composable
private fun BannerCard(item: BaseItemDto, modifier: Modifier, onFocus: () -> Unit, onClick: () -> Unit) {
	val api = koinInject<ApiClient>()
	val interaction = remember { MutableInteractionSource() }
	val isFocused by interaction.collectIsFocusedAsState()
	val image = remember(item.id) { heroBackdropImage(item) }
	Box(
		modifier
			.aspectRatio(16f / 9f)
			.onFocusChanged { if (it.isFocused) onFocus() }
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.scale(if (isFocused) 1.08f else 1f)
			.clip(RoundedCornerShape(8.dp))
			.background(Color(0xFF262626))
	) {
		if (image != null) {
			coil3.compose.AsyncImage(
				model = image.getUrl(api, maxWidth = 400),
				contentDescription = item.name,
				imageLoader = koinInject<coil3.ImageLoader>(),
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxSize(),
			)
		} else {
			Text(
				item.name.orEmpty(),
				color = Color(0xFFB3B3B3),
				textAlign = TextAlign.Center,
				maxLines = 3,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.align(Alignment.Center).padding(8.dp),
			)
		}
		Box(Modifier.fillMaxSize().border(3.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp)))
	}
}

@Composable
private fun FilterButton(label: String, active: Boolean, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val isFocused by interaction.collectIsFocusedAsState()
	Box(
		Modifier
			.clip(RoundedCornerShape(8.dp))
			.background(if (isFocused) Color.White else Color.White.copy(alpha = if (active) 0.28f else 0.12f))
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 8.dp)
	) {
		Text((if (active) "✓  " else "") + label, color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
	}
}
