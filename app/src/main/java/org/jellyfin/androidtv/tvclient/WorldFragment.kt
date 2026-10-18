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

/** Jellyfin-TV-Client: Seite einer Welt (z. B. Marvel) mit eigenem Hintergrund und Reihen. */
class WorldFragment : Fragment() {
	companion object {
		const val ARG_KEY = "world_key"
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			WorldScreen(requireArguments().getString(ARG_KEY).orEmpty())
		}
	}
}

@Composable
private fun WorldScreen(key: String) {
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val navigationRepository = koinInject<NavigationRepository>()
	val context = LocalContext.current
	var world by remember { mutableStateOf<Worlds.World?>(null) }
	var sections by remember { mutableStateOf<List<Worlds.Section>?>(null) }
	val firstFocus = remember { FocusRequester() }

	LaunchedEffect(key) {
		val userId = userRepository.currentUser.value?.id ?: run { sections = emptyList(); return@LaunchedEffect }
		val found = Worlds.find(api, userId, key) ?: run { sections = emptyList(); return@LaunchedEffect }
		world = found
		val labels = Worlds.Labels(
			forYou = context.getString(R.string.tvclient_world_for_you),
			latest = context.getString(R.string.tvclient_world_latest),
			movies = context.getString(R.string.lbl_movies),
			series = context.getString(R.string.tvclient_world_series, found.name),
		)
		sections = Worlds.sections(api, userId, found, labels)
	}
	LaunchedEffect(sections) {
		if (!sections.isNullOrEmpty()) {
			delay(100)
			runCatching { firstFocus.requestFocus() }
		}
	}

	val accent = Color(world?.definition?.color ?: 0xFF333333)
	// Hintergrund: Bild des ersten Titels mit Hintergrundbild
	val backdrop = sections.orEmpty().asSequence().flatMap { it.items.asSequence() }.firstNotNullOfOrNull(::heroBackdropImage)

	Box(Modifier.fillMaxSize().background(Color.Black)) {
		Box(
			Modifier
				.fillMaxSize()
				.background(Brush.verticalGradient(0f to accent.copy(alpha = 0.55f), 0.6f to Color.Black))
		)
		if (backdrop != null) {
			Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.7f).fillMaxHeight(0.6f)) {
				coil3.compose.AsyncImage(
					model = backdrop.getUrl(api, maxWidth = 1280),
					contentDescription = null,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
				Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black, 0.45f to Color.Black.copy(alpha = 0.5f), 0.8f to Color.Transparent)))
				Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black)))
			}
		}

		// Kopf (Logo/Name) bleibt fest oben, nur die Reihen darunter scrollen – sonst schiebt der Fokus auf
		// die erste Reihe den Namen aus dem Bild
		Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
			// Kopf: Logo des Studios, sonst der Name
			val current = world
			var logoLoaded by remember(current?.imageUrl) { mutableStateOf(false) }
			var logoDark by remember(current?.imageUrl) { mutableStateOf(false) }
			if (current?.imageUrl != null) {
				val request = remember(current.imageUrl) {
					coil3.request.ImageRequest.Builder(context).data(current.imageUrl).allowHardware(false).build()
				}
				coil3.compose.AsyncImage(
					model = request,
					contentDescription = current.name,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Fit,
					alignment = Alignment.CenterStart,
					onSuccess = { state ->
						logoLoaded = true
						logoDark = LogoTint.isDark(state.result.image.toBitmap())
					},
					// Nur dunkle Logos weiß einfärben, farbige/helle bleiben unverändert
					colorFilter = if (logoDark) androidx.compose.ui.graphics.ColorFilter.tint(Color.White) else null,
					modifier = Modifier.heightIn(max = 90.dp).widthIn(max = 320.dp),
				)
			}
			// Name immer zeigen (ein dunkles Logo wäre auf dem dunklen Hintergrund unsichtbar)
			Text(
				current?.name.orEmpty(),
				fontSize = if (logoLoaded) 22.sp else 40.sp,
				fontWeight = FontWeight.Bold,
				color = Color.White,
				modifier = Modifier.padding(top = if (logoLoaded) 6.dp else 0.dp),
			)
			Spacer(Modifier.height(120.dp))
			Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
				when {
					sections == null -> Text(stringResource(R.string.loading), color = Color(0xFFB3B3B3))
					sections!!.isEmpty() -> Text(stringResource(R.string.tvclient_world_empty), color = Color(0xFFB3B3B3))
					else -> sections!!.forEachIndexed { index, section ->
						WorldRow(section, if (index == 0) firstFocus else null) { item ->
							navigationRepository.navigate(Destinations.itemDetails(item.id))
						}
					}
				}
				Spacer(Modifier.height(24.dp))
			}
		}
	}
}

@Composable
private fun WorldRow(section: Worlds.Section, focus: FocusRequester?, onClick: (BaseItemDto) -> Unit) {
	Spacer(Modifier.height(16.dp))
	Text(section.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
	Spacer(Modifier.height(4.dp))
	val first = remember { FocusRequester() }
	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()
	MinimalScroll {
		LazyRow(
			state = listState,
			modifier = Modifier.focusRestorer(first).focusGroup(),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			contentPadding = PaddingValues(12.dp),
		) {
			items(section.items, key = { it.id }) { item ->
				var modifier = Modifier.width(120.dp)
				if (item == section.items.first()) {
					modifier = modifier.focusRequester(first).firstItemScroll(listState, scope)
					if (focus != null) modifier = modifier.focusRequester(focus)
				}
				PosterCard(item, modifier) { onClick(item) }
			}
		}
	}
}

@Composable
private fun PosterCard(item: BaseItemDto, modifier: Modifier, onClick: () -> Unit) {
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
				Text(
					item.name.orEmpty(),
					color = Color(0xFFB3B3B3),
					textAlign = TextAlign.Center,
					modifier = Modifier.align(Alignment.Center).padding(8.dp),
				)
			}
			Box(Modifier.fillMaxSize().border(3.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(6.dp)))
		}
		Spacer(Modifier.height(8.dp))
		Box(Modifier.height(38.dp)) {
			Text(
				item.name.orEmpty(),
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				fontSize = 14.sp,
				lineHeight = 18.sp,
				color = if (focused) Color.White else Color(0xFFB3B3B3),
			)
		}
	}
}
