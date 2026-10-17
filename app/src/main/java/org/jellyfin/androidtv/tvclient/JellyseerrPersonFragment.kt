package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import timber.log.Timber

/** Jellyfin-TV-Client: Personenseite (Schauspieler) aus Jellyseerr mit Biografie und Filmografie. */
class JellyseerrPersonFragment : Fragment() {
	companion object {
		const val ARG_ID = "person_id"
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			JellyseerrPersonScreen(requireArguments().getInt(ARG_ID))
		}
	}
}

@Composable
private fun JellyseerrPersonScreen(personId: Int) {
	val api = koinInject<ApiClient>()
	val navigationRepository = koinInject<NavigationRepository>()
	var person by remember { mutableStateOf<Jellyseerr.PersonDetails?>(null) }
	var error by remember { mutableStateOf(false) }
	var bioExpanded by remember { mutableStateOf(false) }
	val firstFocus = remember { FocusRequester() }

	LaunchedEffect(personId) {
		if (Jellyseerr.detect(api) == null) {
			error = true
			return@LaunchedEffect
		}
		runCatching { Jellyseerr.person(personId) }
			.onSuccess { person = it }
			.onFailure {
				Timber.w(it, "Person nicht verfügbar")
				error = true
			}
	}
	LaunchedEffect(person) {
		if (person != null) {
			delay(100)
			runCatching { firstFocus.requestFocus() }
		}
	}

	Box(Modifier.fillMaxSize().background(Color(0xFF101010))) {
		val info = person
		if (info == null) {
			Text(
				stringResource(if (error) R.string.tvclient_jellyseerr_error else R.string.tvclient_jellyseerr_loading),
				color = Color(0xFFB3B3B3),
				modifier = Modifier.align(Alignment.Center),
			)
			return@Box
		}

		Column(
			Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 48.dp, vertical = 27.dp),
		) {
			Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
				Box(Modifier.width(160.dp).height(240.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF262626))) {
					info.imageUrl?.let {
						coil3.compose.AsyncImage(
							model = it,
							contentDescription = info.name,
							imageLoader = koinInject<coil3.ImageLoader>(),
							contentScale = ContentScale.Crop,
							modifier = Modifier.fillMaxSize(),
						)
					}
				}
				Column(Modifier.fillMaxWidth(0.8f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
					Text(info.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
					listOfNotNull(info.birthday?.let { formatDate(it) }, info.placeOfBirth).joinToString("  ·  ").takeIf { it.isNotEmpty() }?.let {
						Text(it, color = Color(0xFFB3B3B3))
					}
					info.biography?.let { bio ->
						// Biografie gekürzt; OK klappt sie aus
						val interaction = remember { MutableInteractionSource() }
						val focused by interaction.collectIsFocusedAsState()
						Text(
							bio,
							color = Color(0xFFE0E0E0),
							fontSize = 15.sp,
							maxLines = if (bioExpanded) Int.MAX_VALUE else 6,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier
								.focusRequester(firstFocus)
								.border(2.dp, if (focused) Color.White.copy(alpha = 0.8f) else Color.Transparent, RoundedCornerShape(6.dp))
								.padding(4.dp)
								.clickable(interactionSource = interaction, indication = null) { bioExpanded = !bioExpanded },
						)
					}
				}
			}

			CreditRow(R.string.tvclient_jellyseerr_movies, info.movies, if (info.biography == null) firstFocus else null) { media ->
				navigationRepository.navigate(Destinations.jellyseerrDetails(media.type, media.id))
			}
			CreditRow(R.string.tvclient_jellyseerr_series, info.series, if (info.biography == null && info.movies.isEmpty()) firstFocus else null) { media ->
				navigationRepository.navigate(Destinations.jellyseerrDetails(media.type, media.id))
			}
			Spacer(Modifier.height(24.dp))
		}
	}
}

@Composable
private fun CreditRow(title: Int, items: List<Jellyseerr.Media>, focus: FocusRequester?, onClick: (Jellyseerr.Media) -> Unit) {
	if (items.isEmpty()) return
	Spacer(Modifier.height(20.dp))
	Text(stringResource(title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
	Spacer(Modifier.height(4.dp))
	// Von oben kommend immer auf den ersten Titel (danach auf den zuletzt gewählten), linksbündig
	val first = remember { FocusRequester() }
	MinimalScroll {
		LazyRow(
			modifier = Modifier.focusRestorer(first).focusGroup(),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			contentPadding = PaddingValues(12.dp),
		) {
			items(items, key = { "${it.type}-${it.id}" }) { media ->
				var modifier = Modifier.width(120.dp)
				if (media == items.first()) {
					modifier = modifier.focusRequester(first)
					if (focus != null) modifier = modifier.focusRequester(focus)
				}
				MediaCard(media, modifier) { onClick(media) }
			}
		}
	}
}

/** „1952-06-07“ → „7.6.1952“. */
private fun formatDate(date: String): String {
	val parts = date.split("-")
	return if (parts.size == 3) "${parts[2].trimStart('0')}.${parts[1].trimStart('0')}.${parts[0]}" else date
}
