package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import timber.log.Timber

/** Jellyfin-TV-Client: Filme und Serien bei Jellyseerr suchen und anfragen. */
class JellyseerrFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			JellyseerrScreen()
		}
	}
}

@Composable
private fun JellyseerrScreen() {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val navigationRepository = koinInject<NavigationRepository>()
	var query by remember { mutableStateOf("") }
	val results = remember { mutableStateListOf<Jellyseerr.Media>() }
	var loading by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }
	var selected by remember { mutableStateOf<Jellyseerr.Media?>(null) }
	val inputFocus = remember { FocusRequester() }
	val confirmFocus = remember { FocusRequester() }

	// Ohne Suchbegriff: gerade beliebte Titel; sonst nach kurzer Tipp-Pause suchen
	var available by remember { mutableStateOf<Boolean?>(null) }
	LaunchedEffect(Unit) {
		available = Jellyseerr.detect(api) != null
		if (available != true) {
			error = context.getString(R.string.tvclient_jellyseerr_error)
			loading = false
		}
	}
	LaunchedEffect(query, available) {
		if (available != true) return@LaunchedEffect
		if (query.isBlank() && !Jellyseerr.supportsTrending) {
			results.clear()
			loading = false
			return@LaunchedEffect
		}
		if (query.isNotBlank()) delay(500)
		loading = true
		error = null
		runCatching { if (query.isBlank()) Jellyseerr.trending() else Jellyseerr.search(query.trim()) }
			.onSuccess { results.clear(); results.addAll(it) }
			.onFailure {
				Timber.w(it, "Jellyseerr-Abfrage fehlgeschlagen")
				error = context.getString(R.string.tvclient_jellyseerr_error)
			}
		loading = false
	}
	LaunchedEffect(Unit) { runCatching { inputFocus.requestFocus() } }
	LaunchedEffect(selected) { if (selected != null) runCatching { delay(50); confirmFocus.requestFocus() } }

	Box(Modifier.fillMaxSize().background(Color(0xFF101010))) {
		Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
			Text(stringResource(R.string.tvclient_jellyseerr_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
			Spacer(Modifier.height(12.dp))
			SearchTextInput(
				query = query,
				onQueryChange = { query = it },
				onQuerySubmit = {},
				modifier = Modifier.fillMaxWidth().focusRequester(inputFocus),
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = when {
					error != null -> error!!
					loading -> stringResource(R.string.tvclient_jellyseerr_loading)
					query.isBlank() && Jellyseerr.supportsTrending -> stringResource(R.string.tvclient_jellyseerr_trending)
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
				items(results, key = { "${it.type}-${it.id}" }) { media ->
					MediaCard(media) {
						val jellyfinId = media.jellyfinId
						when {
							// Schon da: direkt zum Titel in Jellyfin
							media.status in setOf(Jellyseerr.Status.AVAILABLE, Jellyseerr.Status.PARTIAL) && jellyfinId != null ->
								navigationRepository.navigate(Destinations.itemDetails(jellyfinId))
							media.status == Jellyseerr.Status.NONE -> selected = media
							else -> Toast.makeText(context, R.string.tvclient_jellyseerr_already, Toast.LENGTH_SHORT).show()
						}
					}
				}
			}
		}

		// Nachfrage vor dem Anfragen
		selected?.let { media ->
			Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
				Column(
					Modifier.width(520.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF202124)).padding(24.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp),
				) {
					Text(
						stringResource(R.string.tvclient_jellyseerr_confirm, media.title + (media.year?.let { " ($it)" } ?: "")),
						fontSize = 20.sp, color = Color.White,
					)
					media.overview?.let { Text(it, maxLines = 4, color = Color(0xFFB3B3B3)) }
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						Button(
							onClick = {
								scope.launch {
									runCatching { Jellyseerr.request(media, userRepository.currentUser.value?.id) }
										.onSuccess {
											Toast.makeText(context, context.getString(R.string.tvclient_jellyseerr_requested, media.title), Toast.LENGTH_LONG).show()
											val index = results.indexOf(media)
											if (index >= 0) results[index] = media.copy(status = Jellyseerr.Status.REQUESTED)
										}
										.onFailure {
											Timber.w(it, "Jellyseerr-Anfrage fehlgeschlagen")
											val message = when ((it as? Jellyseerr.RequestException)?.code) {
												409 -> R.string.tvclient_jellyseerr_already
												403 -> R.string.tvclient_jellyseerr_no_permission
												else -> R.string.tvclient_jellyseerr_error
											}
											Toast.makeText(context, message, Toast.LENGTH_LONG).show()
										}
									selected = null
								}
							},
							modifier = Modifier.focusRequester(confirmFocus),
						) { Text(stringResource(R.string.tvclient_jellyseerr_request)) }
						Button(onClick = { selected = null }) { Text(stringResource(R.string.lbl_cancel)) }
					}
				}
			}
		}
	}
}

@Composable
private fun MediaCard(media: Jellyseerr.Media, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	Column(
		Modifier
			.scale(if (focused) 1.06f else 1f)
			.focusable(interactionSource = interaction)
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
			AsyncImage(
				modifier = Modifier.fillMaxSize(),
				url = media.posterUrl,
				aspectRatio = 2f / 3f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
			val badge = when (media.status) {
				Jellyseerr.Status.AVAILABLE -> stringResource(R.string.tvclient_jellyseerr_available) to Color(0xFF2E7D32)
				Jellyseerr.Status.PARTIAL -> stringResource(R.string.tvclient_jellyseerr_partial) to Color(0xFF1F5FBF)
				Jellyseerr.Status.REQUESTED -> stringResource(R.string.tvclient_jellyseerr_pending) to Color(0xFFC9961A)
				Jellyseerr.Status.NONE -> null
			}
			badge?.let { (label, color) ->
				Text(
					label,
					fontSize = 12.sp,
					color = Color.White,
					modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp),
				)
			}
		}
		Spacer(Modifier.height(6.dp))
		Text(media.title, maxLines = 1, color = if (focused) Color.White else Color(0xFFB3B3B3))
		media.year?.let { Text(it, fontSize = 12.sp, color = Color(0xFF808080)) }
	}
}
