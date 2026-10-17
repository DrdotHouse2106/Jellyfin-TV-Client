package org.jellyfin.androidtv.tvclient

import android.app.AlertDialog
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Detailseite eines Jellyseerr-Treffers – zum Prüfen, ob es der richtige Titel ist,
 * bevor man ihn anfragt. Zeigt Bild, Eckdaten, Inhalt, Regie/Idee, Besetzung und bei Serien die Staffeln.
 */
class JellyseerrDetailsFragment : Fragment() {
	companion object {
		const val ARG_TYPE = "type"
		const val ARG_ID = "tmdb_id"
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			JellyseerrDetailsScreen(
				type = requireArguments().getString(ARG_TYPE) ?: "movie",
				tmdbId = requireArguments().getInt(ARG_ID),
			)
		}
	}
}

@Composable
private fun JellyseerrDetailsScreen(type: String, tmdbId: Int) {
	val context = LocalContext.current
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val navigationRepository = koinInject<NavigationRepository>()
	val scope = rememberCoroutineScope()
	var details by remember { mutableStateOf<Jellyseerr.Details?>(null) }
	var related by remember { mutableStateOf<List<Jellyseerr.Media>>(emptyList()) }
	var jellyfinItem by remember { mutableStateOf<BaseItemDto?>(null) }
	var localTrailerUrl by remember { mutableStateOf<String?>(null) }
	// Automatische Trailer-Vorschau oben rechts; aus, sobald das große Fenster oder ein Dialog aufgeht
	var previewOn by remember { mutableStateOf(true) }
	val scrollState = rememberScrollState()
	var justRequested by remember { mutableStateOf(false) }
	var belowFocused by remember { mutableStateOf(false) }
	var autoTrailerFailed by remember { mutableStateOf(false) }
	val belowFocus = Modifier.onFocusChanged { if (it.hasFocus) belowFocused = true }
	val requestedFocus = remember { FocusRequester() }
	val playbackLauncher = koinInject<PlaybackLauncher>()
	var error by remember { mutableStateOf(false) }
	val primaryFocus = remember { FocusRequester() }

	LaunchedEffect(type, tmdbId) {
		if (Jellyseerr.detect(api) == null) {
			error = true
			return@LaunchedEffect
		}
		runCatching { Jellyseerr.details(type, tmdbId) }
			.onSuccess { details = it }
			.onFailure {
				Timber.w(it, "Jellyseerr-Details nicht verfügbar")
				error = true
			}
	}
	LaunchedEffect(details != null) {
		val info = details ?: return@LaunchedEffect
		// Den Titel in Jellyfin finden (die gespeicherte ID kann nach Umbenennen veraltet sein)
		if (info.media.status in setOf(Jellyseerr.Status.AVAILABLE, Jellyseerr.Status.PARTIAL)) {
			jellyfinItem = JellyfinLink.resolve(api, info.media, userRepository.currentUser.value?.id)
			jellyfinItem?.takeIf { (it.localTrailerCount ?: 0) > 0 }?.let { localTrailerUrl = TrailerPreview.streamUrl(api, it) }
		}
		related = runCatching { Jellyseerr.recommendations(type, tmdbId) }.getOrDefault(emptyList())
	}
	LaunchedEffect(details) {
		if (details != null) {
			delay(100)
			runCatching { primaryFocus.requestFocus() }
			delay(50)
			scrollState.scrollTo(0)
		}
	}

	fun request(info: Jellyseerr.Details, seasons: List<Int>?) {
		scope.launch {
			runCatching { Jellyseerr.request(info.media, userRepository.currentUser.value?.id, seasons) }
				.onSuccess {
					Toast.makeText(context, context.getString(R.string.tvclient_jellyseerr_requested, info.media.title), Toast.LENGTH_LONG).show()
					justRequested = true
					details = info.copy(
						media = info.media.copy(status = Jellyseerr.Status.REQUESTED),
						seasons = info.seasons.map { if (seasons == null || it.number in seasons) it.copy(status = Jellyseerr.Status.REQUESTED) else it },
					)
					// Fokus auf den nächsten Knopf (Trailer), Seite bleibt oben
					scope.launch { delay(50); runCatching { requestedFocus.requestFocus() }; scrollState.scrollTo(0) }
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
		}
	}

	fun chooseSeasons(info: Jellyseerr.Details) {
		val open = info.seasons.filter { it.status == Jellyseerr.Status.NONE }
		if (open.isEmpty()) return
		previewOn = false
		val checked = BooleanArray(open.size) { true }
		AlertDialog.Builder(context)
			.setTitle(R.string.tvclient_jellyseerr_choose_seasons)
			.setMultiChoiceItems(open.map { it.name }.toTypedArray(), checked) { _, which, isChecked -> checked[which] = isChecked }
			.setPositiveButton(R.string.tvclient_jellyseerr_request) { _, _ ->
				val chosen = open.filterIndexed { index, _ -> checked[index] }.map { it.number }
				if (chosen.isNotEmpty()) request(info, chosen)
			}
			.setNegativeButton(R.string.lbl_cancel, null)
			.show()
	}

	Box(Modifier.fillMaxSize().background(Color(0xFF101010))) {
		val info = details
		if (info == null) {
			Text(
				stringResource(if (error) R.string.tvclient_jellyseerr_error else R.string.tvclient_jellyseerr_loading),
				color = Color(0xFFB3B3B3),
				modifier = Modifier.align(Alignment.Center),
			)
			return@Box
		}

		// Hintergrundbild oben rechts mit Verlauf
		info.backdropUrl?.let { url ->
			AsyncImage(
				modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.7f).fillMaxHeight(0.7f),
				url = url,
				aspectRatio = 16f / 9f,
				scaleType = ImageView.ScaleType.CENTER_CROP,
			)
			Box(
				Modifier.fillMaxSize().background(
					Brush.horizontalGradient(0.3f to Color(0xFF101010), 0.65f to Color(0xCC101010), 1f to Color.Transparent)
				)
			)
			Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 0.75f to Color(0xFF101010))))
		}

		// Trailer im Hintergrund oben rechts (lokal bevorzugt, sonst YouTube)
		BackdropTrailer(
			localUrl = localTrailerUrl,
			youtubeKey = info.trailerKey.takeIf { localTrailerUrl == null },
			background = Color(0xFF101010),
			// Beim Herunterscrollen ausblenden, damit nichts unter den Player rutscht
			// Ausblenden, solange der Fokus unterhalb der Knöpfe ist (Inhalt, Staffeln, Besetzung …)
			enabled = previewOn && !belowFocused,
			onUnavailable = { autoTrailerFailed = true },
			label = info.trailerLabel.takeIf { localTrailerUrl == null },
		)

		Column(
			Modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
				.padding(horizontal = 48.dp, vertical = 27.dp),
		) {
			Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
				// Plakat
				Box(Modifier.width(160.dp).height(240.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF262626))) {
					info.media.posterUrl?.let {
						AsyncImage(modifier = Modifier.fillMaxSize(), url = it, aspectRatio = 2f / 3f, scaleType = ImageView.ScaleType.CENTER_CROP)
					}
				}

				// Platz rechts lassen für den Trailer oben rechts
				Column(Modifier.fillMaxWidth(0.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
					Text(
						info.media.title,
						fontSize = if (info.media.title.length > 26) 26.sp else 32.sp,
						fontWeight = FontWeight.Bold,
						color = Color.White,
						maxLines = 2,
					)
					info.originalTitle?.let { Text(it, color = Color(0xFFB3B3B3)) }
					// Status in der Eckdaten-Zeile, damit sich die Höhe nach einer Anfrage nicht ändert
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					StatusBadge(info.media.status)
					Text(
						listOfNotNull(
							info.media.year,
							info.ageRating?.let { "FSK $it" },
							info.runtimeMinutes?.let { context.getString(R.string.tvclient_jellyseerr_minutes, it) },
							info.seasonCount?.let { context.resources.getQuantityString(R.plurals.tvclient_jellyseerr_seasons, it, it) },
							info.rating?.let { "★ %.1f".format(it) },
						).joinToString("  ·  "),
						color = Color(0xFFD0D0D0),
					)
					}
					if (info.genres.isNotEmpty()) Text(info.genres.joinToString(", "), color = Color(0xFFB3B3B3))
					if (info.creators.isNotEmpty()) Text(
						stringResource(
							if (info.media.type == "movie") R.string.tvclient_jellyseerr_director else R.string.tvclient_jellyseerr_creator,
							info.creators.joinToString(", "),
						),
						color = Color(0xFFB3B3B3),
					)
					Spacer(Modifier.height(8.dp))

					// Knöpfe – solange sie ausgewählt sind, bleibt die Seite ganz oben
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.onFocusChanged { state ->
							if (state.hasFocus) {
								belowFocused = false
								scope.launch { delay(30); scrollState.animateScrollTo(0) }
							}
						},
					) {
						val jellyfinId = jellyfinItem?.id
						if (jellyfinId != null) {
							Button(
								onClick = { navigationRepository.navigate(Destinations.itemDetails(jellyfinId)) },
								modifier = Modifier.focusRequester(primaryFocus),
							) { Text(stringResource(R.string.tvclient_jellyseerr_open)) }
						}
						val canRequest = info.media.status == Jellyseerr.Status.NONE ||
							(info.media.type == "tv" && info.seasons.any { it.status == Jellyseerr.Status.NONE })
						if (canRequest) {
							Button(
								onClick = { request(info, if (info.media.type == "tv") info.seasons.filter { it.status == Jellyseerr.Status.NONE }.map { it.number }.ifEmpty { null } else null) },
								modifier = if (jellyfinId == null || info.media.status == Jellyseerr.Status.NONE) Modifier.focusRequester(primaryFocus) else Modifier,
							) { Text(stringResource(if (info.media.type == "tv") R.string.tvclient_jellyseerr_request_all else R.string.tvclient_jellyseerr_request)) }
							if (info.media.type == "tv" && info.seasons.count { it.status == Jellyseerr.Status.NONE } > 1) {
								Button(onClick = { chooseSeasons(info) }) { Text(stringResource(R.string.tvclient_jellyseerr_choose_seasons)) }
							}
						}
						val localTrailer = (jellyfinItem?.localTrailerCount ?: 0) > 0
						// Läuft der Trailer schon automatisch oben rechts, braucht es den Knopf nicht
						val autoTrailer = !autoTrailerFailed && TrailerAutoplay.possible(localTrailerUrl != null, info.trailerKey)
						val showTrailerButton = (localTrailer || info.trailerKey != null) && !autoTrailer
						if (showTrailerButton) {
							Button(
								onClick = {
									previewOn = false
									val item = jellyfinItem
									if (localTrailer && item != null) {
										// Trailer, der auf dem Server liegt (z. B. von Trailarr)
										scope.launch { HomeBillboard.playTrailer(context, api, playbackLauncher, item) }
									} else info.trailerKey?.let { key -> TrailerWindow.showYoutube(context, key) }
								},
								modifier = (if (!canRequest && jellyfinId == null) Modifier.focusRequester(primaryFocus) else Modifier)
									.focusRequester(requestedFocus),
							) { Text(stringResource(R.string.lbl_play_trailers)) }
						}
						if (!canRequest && !showTrailerButton && jellyfinId == null) {
							Button(onClick = { navigationRepository.goBack() }, modifier = Modifier.focusRequester(primaryFocus).focusRequester(requestedFocus)) {
								Text(stringResource(R.string.tvclient_back))
							}
						}
					}
				}
			}

			Spacer(Modifier.height(20.dp))
			// Fokussierbar, damit man mit „Runter“ weiterblättern kann
			info.media.overview?.let { Text(it, color = Color(0xFFE0E0E0), fontSize = 16.sp, modifier = Modifier.fillMaxWidth(0.55f).then(belowFocus).focusFrame()) }

			// Staffeln
			if (info.seasons.isNotEmpty()) {
				Spacer(Modifier.height(20.dp))
				Text(stringResource(R.string.tvclient_jellyseerr_seasons_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
				Spacer(Modifier.height(6.dp))
				info.seasons.forEach { season ->
					Row(belowFocus.focusFrame(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
						Text(
							listOfNotNull(season.name, season.episodes?.let { context.resources.getQuantityString(R.plurals.tvclient_jellyseerr_episodes, it, it) }).joinToString(" · "),
							color = Color(0xFFD0D0D0),
						)
						StatusBadge(season.status)
					}
				}
			}

			// Besetzung
			if (info.cast.isNotEmpty()) {
				Spacer(Modifier.height(20.dp))
				Text(stringResource(R.string.tvclient_jellyseerr_cast), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
				Spacer(Modifier.height(8.dp))
				LazyRow(belowFocus, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
					items(info.cast, key = { it.name + it.role }) { person ->
						// OK öffnet die Personenseite
						Column(
							Modifier.width(90.dp).focusFrame { person.id?.let { navigationRepository.navigate(Destinations.jellyseerrPerson(it)) } },
							horizontalAlignment = Alignment.CenterHorizontally,
						) {
							Box(Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF262626)), contentAlignment = Alignment.Center) {
								if (person.imageUrl != null) {
									AsyncImage(modifier = Modifier.fillMaxSize(), url = person.imageUrl, aspectRatio = 1f, scaleType = ImageView.ScaleType.CENTER_CROP)
								} else {
									// Ohne Foto: Initialen
									Text(
										person.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) },
										color = Color(0xFFB3B3B3), fontSize = 22.sp, fontWeight = FontWeight.Bold,
									)
								}
							}
							Text(person.name, fontSize = 12.sp, color = Color.White, maxLines = 2)
							person.role?.let { Text(it, fontSize = 11.sp, color = Color(0xFF909090), maxLines = 1) }
						}
					}
				}
			}
			// Das könnte dir auch gefallen
			if (related.isNotEmpty()) {
				Spacer(Modifier.height(20.dp))
				Text(stringResource(R.string.tvclient_jellyseerr_related), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
				Spacer(Modifier.height(8.dp))
				LazyRow(
					modifier = belowFocus,
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
				) {
					items(related.take(20), key = { "${it.type}-${it.id}" }) { media ->
						MediaCard(media, Modifier.width(120.dp)) {
							navigationRepository.navigate(Destinations.jellyseerrDetails(media.type, media.id))
						}
					}
				}
			}
			Spacer(Modifier.height(24.dp))
		}
	}
}

/** Fokussierbarer Bereich mit dünnem Rahmen – damit „Runter“ die Seite weiterscrollt. */
@Composable
private fun Modifier.focusFrame(onClick: (() -> Unit)? = null): Modifier {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	return this
		.border(2.dp, if (focused) Color.White.copy(alpha = 0.8f) else Color.Transparent, RoundedCornerShape(6.dp))
		.padding(4.dp)
		.let {
			if (onClick != null) it.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			else it.focusable(interactionSource = interaction)
		}
}
