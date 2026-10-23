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
	// Zurück von einer Detailseite: wieder auf die zuletzt gewählte Karte statt auf die Biografie
	val page = "person-$personId"
	val restoreKey = remember { PageFocus.take(page) }
	val restore = remember { FocusRequester() }

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
			val split = splitByLibrary(person!!.movies, person!!.series)
			val present = listOf(split.library, split.movies, split.series).withIndex()
				.any { (row, items) -> items.any { "$row:${it.type}-${it.id}" == restoreKey } }
			restoreFocus(restore, firstFocus, present)
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

			// Zuerst, was schon in der Bibliothek ist, darunter die weiteren Filme und Serien (zum Anfragen)
			val split = splitByLibrary(info.movies, info.series)
			// Titel aus der Bibliothek direkt auf ihre Detailseite, die anderen auf die Anfrage-Seite
			val open = { key: String, media: Jellyseerr.Media ->
				PageFocus.set(page, key)
				navigationRepository.navigate(media.jellyfinId?.let { Destinations.itemDetails(it) } ?: Destinations.jellyseerrDetails(media.type, media.id))
			}
			val focusFirst = info.biography == null
			CreditRow(0, R.string.tvclient_person_in_library, split.library, if (focusFirst) firstFocus else null, restoreKey, restore, open)
			CreditRow(1, R.string.tvclient_person_more_movies, split.movies, if (focusFirst && split.library.isEmpty()) firstFocus else null, restoreKey, restore, open)
			CreditRow(2, R.string.tvclient_person_more_series, split.series, if (focusFirst && split.library.isEmpty() && split.movies.isEmpty()) firstFocus else null, restoreKey, restore, open)
			Spacer(Modifier.height(24.dp))
		}
	}
}

/** Filmografie aufteilen: in der Bibliothek (ganz oder teilweise) und weitere Filme bzw. Serien. */
data class PersonCredits(val library: List<Jellyseerr.Media>, val movies: List<Jellyseerr.Media>, val series: List<Jellyseerr.Media>)

fun splitByLibrary(movies: List<Jellyseerr.Media>, series: List<Jellyseerr.Media>): PersonCredits {
	val inLibrary = { media: Jellyseerr.Media -> media.status == Jellyseerr.Status.AVAILABLE || media.status == Jellyseerr.Status.PARTIAL }
	return PersonCredits(
		library = (movies + series).filter(inLibrary),
		movies = movies.filterNot(inLibrary),
		series = series.filterNot(inLibrary),
	)
}

@Composable
private fun CreditRow(
	row: Int,
	title: Int,
	items: List<Jellyseerr.Media>,
	focus: FocusRequester?,
	restoreKey: String?,
	restore: FocusRequester,
	onClick: (String, Jellyseerr.Media) -> Unit,
) {
	if (items.isEmpty()) return
	Spacer(Modifier.height(20.dp))
	Text(stringResource(title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
	Spacer(Modifier.height(4.dp))
	// Von oben kommend immer auf den ersten Titel (danach auf den zuletzt gewählten), linksbündig
	val first = remember { FocusRequester() }
	val listState = androidx.compose.foundation.lazy.rememberLazyListState()
	val scope = androidx.compose.runtime.rememberCoroutineScope()
	// Schlüssel je Reihe, denn ein Titel kann in mehreren Reihen stehen
	val keys = remember(items) { items.map { "$row:${it.type}-${it.id}" } }
	RestoreRowScroll(listState, keys, restoreKey)
	MinimalScroll {
		LazyRow(
			state = listState,
			modifier = Modifier.focusRestorer(first).focusGroup(),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			contentPadding = PaddingValues(12.dp),
		) {
			items(items, key = { "${it.type}-${it.id}" }) { media ->
				val key = "$row:${media.type}-${media.id}"
				var modifier = Modifier.width(120.dp)
				if (key == restoreKey) modifier = modifier.focusRequester(restore)
				if (media == items.first()) {
					// Erster Titel: ganz an den Anfang scrollen (sonst wird der Innenabstand weggescrollt)
					modifier = modifier.focusRequester(first).firstItemScroll(listState, scope)
					if (focus != null) modifier = modifier.focusRequester(focus)
				}
				MediaCard(media, modifier) { onClick(key, media) }
			}
		}
	}
}

/** „1952-06-07“ → „7.6.1952“. */
private fun formatDate(date: String): String {
	val parts = date.split("-")
	return if (parts.size == 3) "${parts[2].trimStart('0')}.${parts[1].trimStart('0')}.${parts[0]}" else date
}
