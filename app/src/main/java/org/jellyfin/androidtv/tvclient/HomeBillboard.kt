package org.jellyfin.androidtv.tvclient

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.querying.GetLocalRecommendationsRequest
import org.jellyfin.androidtv.data.querying.LocalRecommendationsType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.androidtv.util.sdk.TrailerUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Jellyfin-TV-Client: Billboard ganz oben (wie bei Netflix) – wechselnde Vorschläge mit den
 * Knöpfen „Abspielen“, „Mehr Infos“ und „Trailer“. Solange die Knöpfe ausgewählt sind, wechselt
 * der Infobereich alle paar Sekunden zum nächsten Vorschlag.
 */
object HomeBillboard {
	private const val MAX_ITEMS = 8
	val rotationInterval = 9.seconds

	private val fields = ItemRepository.browseFields + setOf(ItemFields.REMOTE_TRAILERS, ItemFields.TAGLINES)

	/** Vorschläge: zuerst aus „Local Recommendations“, sonst die neuesten Filme und Serien. */
	suspend fun load(api: ApiClient): List<BaseItemDto> = withContext(Dispatchers.IO) {
		val recommendedIds = listOf(LocalRecommendationsType.MOVIES, LocalRecommendationsType.SERIES).flatMap { type ->
			runCatching {
				val request = GetLocalRecommendationsRequest(type, limit = MAX_ITEMS / 2)
				api.get<BaseItemDtoQueryResult>(request.path, queryParameters = request.queryParameters).content.items
			}.getOrDefault(emptyList()).map { it.id }
		}

		val items = runCatching {
			if (recommendedIds.isNotEmpty()) {
				val byId = api.itemsApi.getItems(
					GetItemsRequest(ids = recommendedIds, fields = fields, enableUserData = true)
				).content.items.associateBy { it.id }
				// Abwechselnd Film und Serie, in Empfehlungsreihenfolge
				recommendedIds.mapNotNull { byId[it] }
			} else {
				api.itemsApi.getItems(
					GetItemsRequest(
						includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						sortBy = setOf(ItemSortBy.DATE_CREATED),
						sortOrder = setOf(SortOrder.DESCENDING),
						imageTypes = setOf(ImageType.BACKDROP),
						limit = MAX_ITEMS,
						fields = fields,
						enableUserData = true,
					)
				).content.items
			}
		}.onFailure { Timber.w(it, "Billboard konnte nicht geladen werden") }.getOrDefault(emptyList())

		interleave(items.filter { heroBackdropImage(it) != null }).take(MAX_ITEMS)
	}

	/** Filme und Serien abwechselnd. */
	fun interleave(items: List<BaseItemDto>): List<BaseItemDto> {
		val movies = items.filter { it.type != BaseItemKind.SERIES }.toMutableList()
		val series = items.filter { it.type == BaseItemKind.SERIES }.toMutableList()
		return buildList {
			while (movies.isNotEmpty() || series.isNotEmpty()) {
				movies.removeFirstOrNull()?.let(::add)
				series.removeFirstOrNull()?.let(::add)
			}
		}
	}

	fun play(context: Context, playbackHelper: PlaybackHelper, item: BaseItemDto) {
		playbackHelper.retrieveAndPlay(item.id, false, context)
	}

	/** Trailer im kleinen Fenster: zuerst der Trailer vom Server, sonst YouTube. */
	suspend fun playTrailer(context: Context, api: ApiClient, @Suppress("UNUSED_PARAMETER") playbackLauncher: PlaybackLauncher, item: BaseItemDto) {
		if ((item.localTrailerCount ?: 0) > 0) {
			TrailerPreview.streamUrl(api, item)?.let { url ->
				TrailerWindow.showLocal(context, url)
				return
			}
		}

		val key = item.remoteTrailers.orEmpty().firstNotNullOfOrNull { TrailerWindow.youtubeKey(it.url) }
		if (key != null) {
			TrailerWindow.showYoutube(context, key)
			return
		}

		val intent = TrailerUtils.getExternalTrailerIntent(context, item)
		try {
			if (intent != null) context.startActivity(intent)
			else Toast.makeText(context, R.string.tvclient_no_trailer, Toast.LENGTH_SHORT).show()
		} catch (err: ActivityNotFoundException) {
			Timber.w(err, "Trailer konnte nicht geöffnet werden")
			Toast.makeText(context, R.string.no_player_message, Toast.LENGTH_LONG).show()
		}
	}
}

/**
 * Knöpfe unter dem Infobereich. Sie beziehen sich auf den gerade gezeigten Titel.
 */
@Composable
fun HomeBillboardButtons(
	items: List<BaseItemDto>,
	modifier: Modifier = Modifier,
	focusRequester: FocusRequester,
	downFocusRequester: FocusRequester,
	leftFocusRequester: FocusRequester,
) {
	val context = LocalContext.current
	val api = koinInject<ApiClient>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val playbackLauncher = koinInject<PlaybackLauncher>()
	val navigationRepository = koinInject<NavigationRepository>()
	val scope = rememberCoroutineScope()
	val current by HomeHeroState.item.collectAsState()

	var focused by remember { mutableStateOf(false) }
	var index by remember { mutableIntStateOf(0) }

	// Solange die Knöpfe ausgewählt sind: Vorschläge durchwechseln
	LaunchedEffect(focused, items) {
		if (!focused || items.isEmpty()) return@LaunchedEffect
		if (current?.id !in items.map { it.id }) HomeHeroState.item.value = items[index % items.size]
		while (true) {
			delay(HomeBillboard.rotationInterval)
			// Läuft der Trailer, erst nach seinem Ende weiterschalten
			HomeHeroState.trailerPlaying.first { !it }
			index = (index + 1) % items.size
			HomeHeroState.item.value = items[index]
		}
	}

	// Links/Rechts am Rand der Knöpfe: vorheriger/nächster Vorschlag
	fun step(delta: Int): Boolean {
		if (items.size < 2) return false
		val shown = items.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
		index = (shown + delta + items.size) % items.size
		HomeHeroState.trailerPlaying.value = false
		HomeHeroState.item.value = items[index]
		return true
	}
	fun Modifier.stepOnKey(key: Key, delta: Int, enabled: () -> Boolean = { true }) = onPreviewKeyEvent { event ->
		event.type == KeyEventType.KeyDown && event.key == key && enabled() && step(delta)
	}

	val item = current ?: return
	val hasTrailer = (item.localTrailerCount ?: 0) > 0 || !item.remoteTrailers.isNullOrEmpty()

	Row(
		modifier = modifier
			.onFocusChanged { focused = it.hasFocus }
			.focusGroup(),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		val buttonFocus = Modifier.focusProperties {
			up = FocusRequester.Cancel
			down = downFocusRequester
		}

		BillboardButton(
			icon = R.drawable.ic_play,
			text = stringResource(R.string.lbl_play),
			modifier = buttonFocus
				.focusRequester(focusRequester)
				.focusProperties { left = leftFocusRequester }
				// Links: vorheriger Vorschlag; beim ersten Vorschlag geht es in die Navigation
				.stepOnKey(Key.DirectionLeft, -1) { items.indexOfFirst { it.id == current?.id } > 0 },
		) { HomeBillboard.play(context, playbackHelper, item) }

		BillboardButton(
			icon = R.drawable.ic_help,
			text = stringResource(R.string.tvclient_more_info),
			// Rechts auf dem letzten Knopf: nächster Vorschlag
			modifier = if (hasTrailer) buttonFocus else buttonFocus.stepOnKey(Key.DirectionRight, 1),
		) { navigationRepository.navigate(Destinations.itemDetails(item.id)) }

		if (hasTrailer) {
			BillboardButton(
				icon = R.drawable.ic_trailer,
				text = stringResource(R.string.lbl_trailers),
				modifier = buttonFocus.stepOnKey(Key.DirectionRight, 1),
			) { scope.launch { HomeBillboard.playTrailer(context, api, playbackLauncher, item) } }
		}

		// Punkte: welcher Vorschlag gerade gezeigt wird
		if (focused && items.size > 1) {
			Spacer(Modifier.width(12.dp))
			val shownIndex = items.indexOfFirst { it.id == item.id }
			items.indices.forEach { i ->
				Box(
					Modifier
						.padding(horizontal = 3.dp)
						.size(if (i == shownIndex) 9.dp else 6.dp)
						.clip(CircleShape)
						.background(if (i == shownIndex) Color.White else Color.White.copy(alpha = 0.4f))
				)
			}
		}
	}
}

@Composable
private fun BillboardButton(
	@DrawableRes icon: Int,
	text: String,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	Button(onClick = onClick, modifier = modifier) {
		Icon(
			imageVector = ImageVector.vectorResource(icon),
			contentDescription = null,
			modifier = Modifier.size(20.dp),
		)
		Spacer(Modifier.width(8.dp))
		Text(text)
	}
}
