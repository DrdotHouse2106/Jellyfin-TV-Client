package org.jellyfin.androidtv.tvclient

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

/** Verlauf, der das Hintergrundbild links und unten abdunkelt, damit Text und Zeilen lesbar bleiben. */
val heroScrim = Brush.horizontalGradient(
	0f to Color.Black.copy(alpha = 0.92f),
	0.45f to Color.Black.copy(alpha = 0.6f),
	1f to Color.Transparent,
)

val heroBottomScrim = Brush.verticalGradient(
	0.35f to Color.Transparent,
	1f to Color.Black.copy(alpha = 0.85f),
)

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
			.fillMaxWidth()
			.padding(start = 48.dp, end = 48.dp, top = 8.dp),
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
				modifier = Modifier.widthIn(max = 720.dp),
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
		if (item.parentIndexNumber != null && item.indexNumber != null) "S${item.parentIndexNumber}:F${item.indexNumber}" else null,
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
