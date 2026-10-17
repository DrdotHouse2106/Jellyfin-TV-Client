package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.tvclient.TrailerVideo
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.ZoomBox
import org.jellyfin.androidtv.ui.composable.modifier.overscan

@Composable
fun DreamContentLibraryShowcase(
	content: DreamContent.LibraryShowcase,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	ZoomBox(
		initialValue = 1f,
		targetValue = 1.1f,
		delayMillis = 1_000,
		durationMillis = 30_000,
	) {
		Image(
			bitmap = content.backdrop.asImageBitmap(),
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize()
		)

		// Image vignette
		DreamContentVignette()
	}

	// Jellyfin-TV-Client: Trailer über dem Bild (Ton wie in den Einstellungen) (blendet sich erst mit dem ersten Bild ein)
	content.trailerUrl?.let { url ->
		TrailerVideo(
			url = url,
			withSound = org.koin.java.KoinJavaComponent.get<org.jellyfin.androidtv.preference.UserPreferences>(org.jellyfin.androidtv.preference.UserPreferences::class.java)[org.jellyfin.androidtv.tvclient.TrailerPreview.soundPreference],
			modifier = Modifier.fillMaxSize(),
		)
		DreamContentVignette()
	}

	// Overlay
	Row(
		modifier = Modifier
			.align(Alignment.BottomStart)
			.overscan(),
	) {
		if (content.logo != null) {
			BoxWithConstraints {
				Image(
					bitmap = content.logo.asImageBitmap(),
					contentDescription = content.item.name,
					modifier = Modifier
						.sizeIn(
							maxWidth = maxWidth * 0.35f,
							maxHeight = 75.dp
						)
				)
			}
		} else {
			Text(
				text = content.item.name.orEmpty(),
				style = TextStyle(
					color = Color.White,
					fontSize = 32.sp
				),
			)
		}
	}
}
