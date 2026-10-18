package org.jellyfin.androidtv.tvclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.compose.koinInject

/**
 * Jellyfin-TV-Client: Infobereich der Startseite, solange eine Welt-Kachel ausgewählt ist – großes
 * Logo, Name und Anzahl der Titel, dahinter das Bild der Welt bzw. nach kurzer Zeit ein Trailer.
 */
@Composable
fun WorldHeroBackdrop(world: Worlds.World) {
	val api = koinInject<ApiClient>()
	val userPreferences = koinInject<UserPreferences>()
	var trailerUrl by remember(world.key) { mutableStateOf<String?>(null) }

	LaunchedEffect(world.key) {
		val trailerItemId = world.trailerItemId?.toUUIDOrNull() ?: return@LaunchedEffect
		if (!userPreferences[TrailerPreview.enabledPreference]) return@LaunchedEffect
		delay(TrailerPreview.delay)
		val item = runCatching { withContext(Dispatchers.IO) { api.userLibraryApi.getItem(itemId = trailerItemId).content } }.getOrNull()
			?: return@LaunchedEffect
		trailerUrl = TrailerPreview.streamUrl(api, item)
	}

	Box(Modifier.fillMaxSize().background(Color.Black)) {
		Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.7f).fillMaxHeight(0.68f)) {
			world.backdropUrl?.let { url ->
				coil3.compose.AsyncImage(
					model = url,
					contentDescription = null,
					imageLoader = koinInject<coil3.ImageLoader>(),
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
			}
			trailerUrl?.let { url ->
				TrailerVideo(
					url = url,
					withSound = userPreferences[TrailerPreview.soundPreference],
					modifier = Modifier.fillMaxSize(),
				)
			}
			Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black, 0.35f to Color.Black.copy(alpha = 0.55f), 0.7f to Color.Transparent)))
			Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black)))
		}
	}
}

@Composable
fun WorldHeroContent(world: Worlds.World) {
	val context = LocalContext.current
	var logoLoaded by remember(world.imageUrl) { mutableStateOf(false) }
	var logoDark by remember(world.imageUrl) { mutableStateOf(false) }

	Column(
		modifier = Modifier.fillMaxWidth(0.5f).padding(start = 28.dp, top = 28.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		world.imageUrl?.let { url ->
			val request = remember(url) { coil3.request.ImageRequest.Builder(context).data(url).allowHardware(false).build() }
			coil3.compose.AsyncImage(
				model = request,
				contentDescription = world.name,
				imageLoader = koinInject<coil3.ImageLoader>(),
				contentScale = ContentScale.Fit,
				alignment = Alignment.CenterStart,
				onSuccess = { state ->
					logoLoaded = true
					logoDark = LogoTint.isDark(state.result.image.toBitmap())
				},
				colorFilter = if (logoDark) ColorFilter.tint(Color.White) else null,
				modifier = Modifier.heightIn(max = 110.dp).widthIn(max = 420.dp),
			)
		}
		Text(
			world.name,
			fontSize = if (logoLoaded) 20.sp else 34.sp,
			fontWeight = FontWeight.Bold,
			color = Color.White,
		)
		Text(
			pluralStringResource(R.plurals.tvclient_world_titles, world.count, world.count),
			fontSize = 15.sp,
			color = Color(0xFFB8BFCC),
		)
	}
}
