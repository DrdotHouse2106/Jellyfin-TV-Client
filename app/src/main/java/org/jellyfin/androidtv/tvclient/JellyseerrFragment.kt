package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import timber.log.Timber

/** Jellyfin-TV-Client: Filme und Serien bei Jellyseerr suchen; OK öffnet die Detailseite zum Anfragen. */
class JellyseerrFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			JellyseerrScreen()
		}
	}
}

/** Letzte Suche, damit sie nach der Detailseite (Zurück) noch da ist. */
internal object JellyseerrSearchState {
	var query: String = ""
	var focusedKey: String? = null

	/** Neu öffnen (über die Navigation): leere Suche, Fokus ins Suchfeld. */
	fun reset() {
		query = ""
		focusedKey = null
	}
}

@Composable
private fun JellyseerrScreen() {
	val context = LocalContext.current
	val api = koinInject<ApiClient>()
	val navigationRepository = koinInject<NavigationRepository>()
	val userRepository = koinInject<UserRepository>()
	val keyboard = LocalSoftwareKeyboardController.current
	var query by remember { mutableStateOf(JellyseerrSearchState.query) }
	val results = remember { mutableStateListOf<Jellyseerr.Media>() }
	var loading by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }
	val inputFocus = remember { FocusRequester() }
	val firstResultFocus = remember { FocusRequester() }
	val restoreFocus = remember { FocusRequester() }
	var available by remember { mutableStateOf<Boolean?>(null) }
	var initialFocusDone by remember { mutableStateOf(false) }

	fun focusResults() {
		if (results.isEmpty()) return
		keyboard?.hide()
		runCatching { firstResultFocus.requestFocus() }
	}

	LaunchedEffect(Unit) {
		available = Jellyseerr.detect(api) != null
		if (available != true) {
			error = context.getString(R.string.tvclient_jellyseerr_error)
			loading = false
		}
	}
	LaunchedEffect(query, available) {
		JellyseerrSearchState.query = query
		if (available != true) return@LaunchedEffect
		if (query.isNotBlank()) delay(500)
		loading = true
		error = null
		runCatching {
			if (query.isNotBlank()) Jellyseerr.search(query.trim())
			// Ohne Suchbegriff: Empfehlungen passend zu den eigenen Vorlieben, neuere zuerst
			else JellyseerrDiscover.forYou(api, userRepository.currentUser.value?.id).ifEmpty { Jellyseerr.trending() }
		}
			.onSuccess { results.clear(); results.addAll(it) }
			.onFailure {
				Timber.w(it, "Jellyseerr-Abfrage fehlgeschlagen")
				error = context.getString(R.string.tvclient_jellyseerr_error)
			}
		loading = false
		// Zurück von der Detailseite: Fokus wieder auf die zuletzt gewählte Karte
		if (JellyseerrSearchState.focusedKey != null) {
			delay(100)
			runCatching { restoreFocus.requestFocus() }
		} else if (query.isBlank() && !initialFocusDone) {
			initialFocusDone = true
			// Beim Öffnen: auf „Für dich“ statt ins Suchfeld (sonst geht sofort die Tastatur auf);
			// ins Suchfeld kommt man mit „Hoch“
			delay(100)
			if (results.isNotEmpty()) runCatching { firstResultFocus.requestFocus() }
			else runCatching { inputFocus.requestFocus() }
		}
	}

	Box(Modifier.fillMaxSize().background(Color(0xFF101010))) {
		Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
			Text(stringResource(R.string.tvclient_jellyseerr_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
			Spacer(Modifier.height(12.dp))
			SearchTextInput(
				query = query,
				onQueryChange = { query = it },
				// Suchen-Taste der Tastatur: Tastatur zu, Fokus auf das erste Ergebnis
				onQuerySubmit = { focusResults() },
				modifier = Modifier
					.fillMaxWidth()
					.focusRequester(inputFocus)
					.onPreviewKeyEvent { event ->
						// Runter verlässt das Suchfeld (das Textfeld behält die Taste sonst für sich)
						if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && results.isNotEmpty()) {
							focusResults()
							true
						} else false
					},
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = when {
					error != null -> error!!
					loading -> stringResource(R.string.tvclient_jellyseerr_loading)
					query.isBlank() && results.isNotEmpty() -> stringResource(R.string.tvclient_jellyseerr_for_you)
					query.isBlank() -> stringResource(R.string.tvclient_jellyseerr_search_hint)
					results.isEmpty() -> stringResource(R.string.tvclient_jellyseerr_nothing)
					else -> ""
				},
				color = Color(0xFFB3B3B3),
			)
			Spacer(Modifier.height(8.dp))
			LazyVerticalGrid(
				columns = GridCells.Adaptive(130.dp),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
				modifier = Modifier.fillMaxSize(),
			) {
				itemsIndexed(results, key = { _, it -> "${it.type}-${it.id}" }) { index, media ->
					val key = "${media.type}-${media.id}"
					val focusModifier = Modifier
						.then(if (index == 0) Modifier.focusRequester(firstResultFocus) else Modifier)
						.then(if (key == JellyseerrSearchState.focusedKey) Modifier.focusRequester(restoreFocus) else Modifier)
					MediaCard(media, focusModifier) {
						JellyseerrSearchState.focusedKey = key
						navigationRepository.navigate(Destinations.jellyseerrDetails(media.type, media.id))
					}
				}
			}
		}
	}
}

@Composable
internal fun MediaCard(media: Jellyseerr.Media, modifier: Modifier = Modifier, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Column(
		modifier
			.scale(if (focused) 1.06f else 1f)
			// clickable ist selbst fokussierbar und reagiert auf OK (DPAD_CENTER) und Enter
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
	) {
		Box(
			Modifier
				.fillMaxWidth()
				.aspectRatio(2f / 3f)
				.clip(RoundedCornerShape(6.dp))
				.background(Color(0xFF262626))
				.border(3.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
		) {
			if (media.posterUrl != null) {
				AsyncImage(
					modifier = Modifier.fillMaxSize(),
					url = media.posterUrl,
					aspectRatio = 2f / 3f,
					scaleType = ImageView.ScaleType.CENTER_CROP,
				)
			} else {
				// Kein Plakat: Titel als Platzhalter
				Text(
					media.title,
					color = Color(0xFFB3B3B3),
					textAlign = TextAlign.Center,
					modifier = Modifier.align(Alignment.Center).padding(8.dp),
				)
			}
			StatusBadge(media.status, Modifier.align(Alignment.TopEnd).padding(6.dp))
		}
		Spacer(Modifier.padding(top = 6.dp))
		Text(media.title, maxLines = 1, color = if (focused) Color.White else Color(0xFFB3B3B3))
		media.year?.let { Text(it, fontSize = 12.sp, color = Color(0xFF808080)) }
	}
}

@Composable
fun StatusBadge(status: Jellyseerr.Status, modifier: Modifier = Modifier) {
	val badge = when (status) {
		Jellyseerr.Status.AVAILABLE -> stringResource(R.string.tvclient_jellyseerr_available) to Color(0xFF2E7D32)
		Jellyseerr.Status.PARTIAL -> stringResource(R.string.tvclient_jellyseerr_partial) to Color(0xFF1F5FBF)
		Jellyseerr.Status.REQUESTED -> stringResource(R.string.tvclient_jellyseerr_pending) to Color(0xFFC9961A)
		Jellyseerr.Status.NONE -> null
	} ?: return
	Text(
		badge.first,
		fontSize = 12.sp,
		color = Color.White,
		modifier = modifier.clip(RoundedCornerShape(4.dp)).background(badge.second).padding(horizontal = 6.dp, vertical = 2.dp),
	)
}
