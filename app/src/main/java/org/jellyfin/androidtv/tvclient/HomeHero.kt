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
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.preference.UserPreferences
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
/** Wie lange der Banner-Fokus „weg“ sein muss, bevor die Vorschau stoppt. */
const val FOCUS_DEBOUNCE_MS = 500L

object HomeHeroState {
	/** Ausgewählter Eintrag; wird von den Startseiten-Zeilen gesetzt. */
	val item = MutableStateFlow<BaseItemDto?>(null)

	/**
	 * Zeilen beim Start sperren, bis der Benutzer eine Taste drückt – sonst holen sich die nachladenden
	 * Leanback-Zeilen den Fokus vom Banner.
	 */
	val rowsFocusBlocked = MutableStateFlow(false)

	/** Läuft gerade eine Trailer-Vorschau? Solange wechselt das Banner nicht automatisch. */
	val trailerPlaying = MutableStateFlow(false)

	/** Ausgewählte Welt-Kachel – dann zeigt der Infobereich die Welt statt eines Titels. */
	val world = MutableStateFlow<Worlds.World?>(null)

	/** Ist die Banner-Knopfzeile ausgewählt? Nur dann läuft die Trailer-Vorschau. */
	val billboardFocused = MutableStateFlow(false)
}

/**
 * Hintergrundbild des ausgewählten Titels oben rechts (etwa zwei Drittel der Breite), das nach links
 * und unten in Schwarz ausläuft – wie bei Netflix. Der Rest der Startseite bleibt schwarz.
 */
@Composable
fun HomeHeroBackdrop(modifier: Modifier = Modifier) {
	val selectedWorld by HomeHeroState.world.collectAsState()
	selectedWorld?.let {
		WorldHeroBackdrop(it)
		return
	}
	val item by HomeHeroState.item.collectAsState()
	val api = koinInject<ApiClient>()
	val userPreferences = koinInject<UserPreferences>()
	val billboardFocusedRaw by HomeHeroState.billboardFocused.collectAsState()
	// Entprellt: beim Wechsel zwischen den Knöpfen ist der Fokus kurz „weg“ – das darf den Trailer nicht neu starten
	var billboardFocused by remember { mutableStateOf(billboardFocusedRaw) }
	LaunchedEffect(billboardFocusedRaw) {
		if (billboardFocusedRaw) billboardFocused = true
		else {
			delay(FOCUS_DEBOUNCE_MS)
			billboardFocused = false
		}
	}

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

			// Trailer-Vorschau: nach kurzer Zeit auf demselben Titel statt des Bildes
			var trailerUrl by remember(current?.id) { mutableStateOf<String?>(null) }
			// Nur solange das Banner ausgewählt ist – steht der Fokus in einer Reihe, bleibt es beim Bild
			LaunchedEffect(current?.id, billboardFocused) {
				trailerUrl = null
				if (!billboardFocused) HomeHeroState.trailerPlaying.value = false
				if (current == null || !billboardFocused || !userPreferences[TrailerPreview.enabledPreference]) return@LaunchedEffect
				val url = TrailerPreview.streamUrl(api, current) ?: return@LaunchedEffect
				// Nach „Zurück“ von der Detailseite gleich an derselben Stelle weiter
				if (TrailerPreview.resumePosition(url) == null) delay(TrailerPreview.delay)
				trailerUrl = url
			}

			if (backdrop != null || trailerUrl != null) {
				Box(Modifier.fillMaxSize()) {
					if (backdrop != null) AsyncImage(
						modifier = Modifier.fillMaxSize(),
						url = backdrop.getUrl(api, maxWidth = 1280),
						blurHash = backdrop.blurHash,
						aspectRatio = 16f / 9f,
						scaleType = ImageView.ScaleType.CENTER_CROP,
					)
					trailerUrl?.let { url ->
						TrailerVideo(
							url = url,
							withSound = userPreferences[TrailerPreview.soundPreference],
							modifier = Modifier.fillMaxSize(),
							onPlayingChange = { HomeHeroState.trailerPlaying.value = it },
						)
					}
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
	val selectedWorld by HomeHeroState.world.collectAsState()
	selectedWorld?.let {
		Box(modifier) { WorldHeroContent(it) }
		return
	}
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
			.padding(start = 28.dp, top = 28.dp),
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

		// Begründung der Empfehlung („Weil du „…“ geschaut hast“), einzeilig
		val reasons by RecommendationReasons.reasons.collectAsState()
		val reason = reasons[RecommendationReasons.key(item.id)]
		reason?.let {
			Text(
				text = it,
				fontSize = 15.sp,
				fontWeight = FontWeight.SemiBold,
				color = Color(RecommendationReasons.COLOR),
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

		// Manche Reihen (z. B. Web-Abschnitte) liefern keine Beschreibung mit – dann kurz nachladen
		val api = koinInject<ApiClient>()
		var overview by remember(item.id) { mutableStateOf(item.overview) }
		LaunchedEffect(item.id) {
			if (overview.isNullOrBlank()) {
				delay(300)
				overview = OverviewCache.get(api, item.id)
			}
		}
		overview?.let(::cleanOverview)?.takeIf { it.isNotBlank() }?.let {
			Text(
				text = it,
				fontSize = 15.sp,
				lineHeight = 21.sp,
				color = Color(0xFFDDE1E8),
				maxLines = if (reason != null) 2 else 3,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth(),
			)
		}
	}
}

/** Nachgeladene Beschreibungen (für Karten ohne Overview), begrenzt auf die letzten Titel. */
object OverviewCache {
	private const val MAX_ENTRIES = 200
	private val cache = object : LinkedHashMap<java.util.UUID, String>(MAX_ENTRIES, 0.75f, true) {
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<java.util.UUID, String>?) = size > MAX_ENTRIES
	}

	suspend fun get(api: ApiClient, id: java.util.UUID): String? {
		synchronized(cache) { cache[id] }?.let { return it }
		val overview = runCatching {
			kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
				api.userLibraryApi.getItem(itemId = id).content.overview
			}
		}.getOrNull() ?: return null
		synchronized(cache) { cache[id] = overview }
		return overview
	}
}

/** Beschreibung ohne Markdown-Links („[Name](http://…)“ → „Name“), HTML-Reste und Zeilenumbrüche. */
fun cleanOverview(text: String): String = text
	// wörtliche „\n“ aus manchen Metadaten-Quellen
	.replace("\\n", " ")
	.replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
	.replace(Regex("<[^>]+>"), "")
	.replace(Regex("\\s+"), " ")
	.trim()

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
