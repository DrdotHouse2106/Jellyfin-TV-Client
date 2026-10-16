package org.jellyfin.androidtv.tvclient

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ImageType
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.microseconds

/**
 * Jellyfin-TV-Client: Infobereich oben auf der Startseite (wie bei Netflix). Zeigt den gerade
 * ausgewählten Titel groß mit Logo bzw. Titel, Eckdaten und Beschreibung.
 */
object HomeHeroState {
	/** Ausgewählter Eintrag; wird von den Startseiten-Zeilen gesetzt. */
	val item = MutableStateFlow<BaseItemDto?>(null)
}

/**
 * Hintergrundbild des ausgewählten Titels oben rechts (etwa zwei Drittel der Breite), das nach links
 * und unten in Schwarz ausläuft – wie bei Netflix. Der Rest der Startseite bleibt schwarz.
 */
@Composable
fun HomeHeroBackdrop(modifier: Modifier = Modifier) {
	val item by HomeHeroState.item.collectAsState()
	val api = koinInject<ApiClient>()

	Box(modifier.fillMaxSize().background(Color.Black)) {
		AnimatedContent(
			targetState = item,
			transitionSpec = { fadeIn() togetherWith fadeOut() },
			contentKey = { it?.id },
			modifier = Modifier
				.align(Alignment.TopEnd)
				.fillMaxWidth(0.7f)
				.fillMaxHeight(0.68f),
			label = "HomeHeroBackdrop",
		) { current ->
			val backdrop = current?.let(::heroBackdropImage)
			if (backdrop != null) {
				Box(Modifier.fillMaxSize()) {
					AsyncImage(
						modifier = Modifier.fillMaxSize(),
						url = backdrop.getUrl(api, maxWidth = 1280),
						blurHash = backdrop.blurHash,
						aspectRatio = 16f / 9f,
						scaleType = ImageView.ScaleType.CENTER_CROP,
					)
					// Nach links und unten in Schwarz auslaufen lassen
					Box(
						Modifier
							.fillMaxSize()
							.background(
								Brush.horizontalGradient(
									0f to Color.Black,
									0.35f to Color.Black.copy(alpha = 0.55f),
									0.7f to Color.Transparent,
								)
							)
					)
					Box(
						Modifier
							.fillMaxSize()
							.background(
								Brush.verticalGradient(
									0.5f to Color.Transparent,
									1f to Color.Black,
								)
							)
					)
				}
			}
		}
	}
}

/** Großes Bild für den Infobereich: Hintergrundbild, sonst Serien-Hintergrund oder Vorschaubild. */
fun heroBackdropImage(item: BaseItemDto): JellyfinImage? =
	item.itemBackdropImages.firstOrNull()
		?: item.parentBackdropImages.firstOrNull()
		?: item.itemImages[ImageType.THUMB]
		?: item.parentImages[ImageType.THUMB]
		?: if (item.type == BaseItemKind.EPISODE || item.type == BaseItemKind.PROGRAM) item.itemImages[ImageType.PRIMARY] else null

@Composable
fun HomeHero(modifier: Modifier = Modifier) {
	val item by HomeHeroState.item.collectAsState()

	AnimatedContent(
		targetState = item,
		transitionSpec = { fadeIn() togetherWith fadeOut() },
		contentKey = { it?.id },
		modifier = modifier,
		label = "HomeHero",
	) { current ->
		if (current != null) HeroContent(current)
	}
}

@Composable
private fun HeroContent(item: BaseItemDto) {
	val imageHelper = koinInject<ImageHelper>()
	val logoUrl = imageHelper.getLogoImageUrl(item, maxWidth = 600)

	Column(
		modifier = Modifier
			.fillMaxWidth(0.5f)
			.padding(start = 48.dp, top = 24.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		if (logoUrl != null) {
			AsyncImage(
				modifier = Modifier
					.heightIn(max = 90.dp)
					.widthIn(max = 420.dp),
				url = logoUrl,
				aspectRatio = 3f,
			)
		} else {
			Text(
				text = heroTitle(item),
				fontSize = 34.sp,
				fontWeight = FontWeight.Bold,
				color = Color.White,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.widthIn(max = 760.dp),
			)
		}

		heroSubtitle(item)?.let {
			Text(
				text = it,
				fontSize = 18.sp,
				fontWeight = FontWeight.SemiBold,
				color = Color.White,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.widthIn(max = 760.dp),
			)
		}

		val facts = heroFacts(item)
		if (facts.isNotEmpty()) {
			Text(
				text = facts.joinToString("  ·  "),
				fontSize = 15.sp,
				color = Color(0xFFB8BFCC),
				maxLines = 1,
			)
		}

		item.overview?.takeIf { it.isNotBlank() }?.let {
			Text(
				text = it,
				fontSize = 15.sp,
				lineHeight = 21.sp,
				color = Color(0xFFDDE1E8),
				maxLines = 3,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth(),
			)
		}
	}
}

/** Haupttitel: bei Folgen der Serienname. */
fun heroTitle(item: BaseItemDto): String = when (item.type) {
	BaseItemKind.EPISODE -> item.seriesName ?: item.name.orEmpty()
	else -> item.name.orEmpty()
}

/** Zweite Zeile: bei Folgen „S1:E3 · Titel“, bei Sendungen Zeit und Sender. */
fun heroSubtitle(item: BaseItemDto): String? = when (item.type) {
	BaseItemKind.EPISODE -> listOfNotNull(
		// Nur echte Staffeln/Folgen (keine Jahres- oder Datumsnummern wie bei YouTube-Kanälen)
		if (item.parentIndexNumber != null && item.indexNumber != null && item.parentIndexNumber!! < 100 && item.indexNumber!! < 1000) {
			"S${item.parentIndexNumber}:F${item.indexNumber}"
		} else null,
		item.name,
	).joinToString(" · ").ifBlank { null }

	BaseItemKind.PROGRAM -> item.episodeTitle
	else -> null
}

/** Eckdaten: Jahr, FSK, Laufzeit, Bewertung, Genres. */
fun heroFacts(item: BaseItemDto): List<String> = buildList {
	item.productionYear?.let { add(it.toString()) }
	item.officialRating?.takeIf { it.isNotBlank() }?.let { add(it) }
	item.runTimeTicks?.takeIf { it > 0 && item.type != BaseItemKind.SERIES }?.let { ticks ->
		val minutes = (ticks / 10).microseconds.inWholeMinutes
		add(if (minutes >= 60) "${minutes / 60} Std. ${minutes % 60} Min." else "$minutes Min.")
	}
	item.communityRating?.let { add("★ " + String.format(java.util.Locale.GERMANY, "%.1f", it)) }
	item.genres?.take(2)?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
}
