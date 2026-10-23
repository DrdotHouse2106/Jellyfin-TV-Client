package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import org.jellyfin.androidtv.util.PlaybackHelper

/**
 * Jellyfin-TV-Client: Seite einer Filmreihe (wie im Web, Plugin Local Recommendations ab 12.45): Bild und
 * Name der Reihe, „x von y gesehen“, Knopf „Weiter mit Teil n“ (spielt ab bzw. setzt fort) und alle Teile in
 * Erscheinungsreihenfolge, gesehene mit Haken.
 */
class MovieSeriesFragment : Fragment() {
	companion object {
		const val ARG_ID = "movie_series_id"
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			MovieSeriesScreen(requireArguments().getString(ARG_ID).orEmpty())
		}
	}
}

@Composable
private fun MovieSeriesScreen(id: String) {
	val api = koinInject<ApiClient>()
	val navigationRepository = koinInject<NavigationRepository>()
	val playbackHelper = koinInject<PlaybackHelper>()
	val context = LocalContext.current
	var detail by remember { mutableStateOf<TvClientSections.SeriesDetail?>(null) }
	var failed by remember { mutableStateOf(false) }
	val continueFocus = remember { FocusRequester() }
	// Zurück von der Detailseite eines Teils: wieder auf diesen Teil statt auf „Weiter mit …“
	val page = "movieseries-$id"
	val restoreKey = remember { PageFocus.take(page) }
	val restore = remember { FocusRequester() }

	LaunchedEffect(id) {
		detail = TvClientSections.seriesDetail(id)
		failed = detail == null
	}
	LaunchedEffect(detail) {
		if (detail?.parts?.isNotEmpty() == true) {
			delay(100)
			restoreFocus(restore, continueFocus, detail?.parts.orEmpty().any { it.id.toString() == restoreKey })
		}
	}

	val series = detail?.series
	val parts = detail?.parts.orEmpty()
	val allWatched = series != null && series.total > 0 && series.watched >= series.total
	val next = parts.getOrNull(if (allWatched) 0 else (series?.nextIndex ?: 0).coerceIn(0, (parts.size - 1).coerceAtLeast(0)))
	val backdrop = series?.imageUrl(api.baseUrl) ?: (next ?: parts.firstOrNull())?.let { heroBackdropImage(it)?.getUrl(api, maxWidth = 1280) }

	Box(Modifier.fillMaxSize().background(Color.Black)) {
		if (backdrop != null) {
			Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.7f).fillMaxHeight(0.6f)) {
				coil3.compose.AsyncImage(
					model = backdrop,
					contentDescription = null,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
				Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black, 0.45f to Color.Black.copy(alpha = 0.5f), 0.8f to Color.Transparent)))
				Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black)))
			}
		}

		Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
			Text(series?.name.orEmpty(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
			when {
				failed -> Text(stringResource(R.string.tvclient_jellyseerr_error), color = Color(0xFFB3B3B3))
				series == null -> Text(stringResource(R.string.loading), color = Color(0xFFB3B3B3))
				else -> Text(series.caption, fontSize = 18.sp, color = Color(0xFFB3B3B3), modifier = Modifier.padding(top = 6.dp))
			}
			if (next != null) {
				Spacer(Modifier.height(20.dp))
				val label = if (allWatched) stringResource(R.string.tvclient_series_from_start)
				else stringResource(R.string.tvclient_series_continue, (series?.nextIndex ?: 0) + 1)
				ContinueButton(label, Modifier.focusRequester(continueFocus)) {
					playbackHelper.retrieveAndPlay(next.id, false, context)
				}
			}
			Spacer(Modifier.height(16.dp))
			Column(Modifier.weight(1f).clipToBounds().verticalScroll(rememberScrollState())) {
				if (parts.isNotEmpty()) {
					Spacer(Modifier.height(16.dp))
					Text(stringResource(R.string.tvclient_series_all_parts, parts.size), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
					Spacer(Modifier.height(4.dp))
					val first = remember { FocusRequester() }
					val listState = rememberLazyListState()
					val scope = rememberCoroutineScope()
					RestoreRowScroll(listState, parts.map { it.id.toString() }, restoreKey)
					MinimalScroll {
						LazyRow(
							state = listState,
							modifier = Modifier.focusRestorer(first).focusGroup(),
							horizontalArrangement = Arrangement.spacedBy(16.dp),
							contentPadding = PaddingValues(12.dp),
						) {
							items(parts, key = { it.id }) { part ->
								var modifier = Modifier.width(120.dp)
								if (part == parts.first()) modifier = modifier.focusRequester(first).firstItemScroll(listState, scope)
								if (part.id.toString() == restoreKey) modifier = modifier.focusRequester(restore)
								PartCard(part, modifier) {
									PageFocus.set(page, part.id.toString())
									navigationRepository.navigate(Destinations.itemDetails(part.id))
								}
							}
						}
					}
				}
				Spacer(Modifier.height(24.dp))
			}
		}
	}
}

@Composable
private fun ContinueButton(label: String, modifier: Modifier, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Box(
		modifier
			.clip(RoundedCornerShape(10.dp))
			.background(if (focused) Color.White else Color.White.copy(alpha = 0.18f))
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 28.dp, vertical = 12.dp)
	) {
		Text("▶  $label", color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
	}
}

@Composable
private fun PartCard(item: BaseItemDto, modifier: Modifier, onClick: () -> Unit) {
	val imageHelper = koinInject<ImageHelper>()
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	val url = remember(item.id) { imageHelper.getPrimaryImageUrl(item, height = 360) }
	Column(modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
		Box(
			Modifier
				.fillMaxWidth()
				.aspectRatio(2f / 3f)
				.scale(if (focused) 1.06f else 1f)
				.clip(RoundedCornerShape(6.dp))
				.background(Color(0xFF262626))
		) {
			if (url != null) {
				coil3.compose.AsyncImage(
					model = url,
					contentDescription = item.name,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
			} else {
				Text(item.name.orEmpty(), color = Color(0xFFB3B3B3), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(8.dp))
			}
			// Gesehen: Haken oben rechts
			if (item.userData?.played == true) {
				Box(
					Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xEE2E7D32)).padding(horizontal = 7.dp, vertical = 2.dp)
				) { Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
			}
			Box(Modifier.fillMaxSize().border(3.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(6.dp)))
		}
		Spacer(Modifier.height(8.dp))
		Box(Modifier.height(38.dp)) {
			Text(item.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 18.sp, color = if (focused) Color.White else Color(0xFFB3B3B3))
		}
	}
}
