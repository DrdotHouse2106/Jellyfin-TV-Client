package org.jellyfin.androidtv.tvclient

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.querying.ExternalItemsRequest
import org.jellyfin.androidtv.ui.home.HomeFragmentLocalRecommendationsRow
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.UUID

/**
 * Jellyfin-TV-Client: weitere Abschnitte des Plugins Local Recommendations (ab 12.45):
 * - Filmreihen („Weiter mit Teil 3“): Startseite und „Weitere Teile der Reihe“ auf der Detailseite eines Films
 * - Saison-Zeile (Weihnachten, Halloween …), nur wenn gerade aktiv
 */
object TvClientSections {
	private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
	private var configFor: String? = null
	private var config: Config = Config()

	data class Config(
		val movieSeries: Boolean = false,
		val movieSeriesTitle: String? = null,
		val seasonal: Boolean = false,
		val moments: Boolean = false,
		val upcoming: Boolean = false,
		/** Titel der Empfehlungszeilen und Schalter „Heute im TV für dich“ wie im Web (Plugin ab 12.48). */
		val moviesTitle: String? = null,
		val seriesTitle: String? = null,
		val liveTvTitle: String? = null,
		val liveTvRow: Boolean = true,
	)

	/** Begründung je Titel („Weiter mit Teil 3“, „4 Teile“): Id ohne Bindestriche → Text, für den Infobereich. */
	val reasons = MutableStateFlow<Map<String, String>>(emptyMap())

	data class MovieSeries(
		val id: String = "",
		val name: String,
		val reason: String?,
		val next: BaseItemDto?,
		val total: Int = 0,
		val watched: Int = 0,
		val nextIndex: Int = 0,
		val hasBackdrop: Boolean = false,
	) {
		/** „1 von 3 gesehen · Weiter mit Teil 2“ bzw. „4 Teile“ (wie im Web). */
		val caption: String
			get() = when {
				watched in 1 until total -> "$watched von $total gesehen · Weiter mit Teil ${nextIndex + 1}"
				total in 1..watched -> "Alle $total Teile gesehen"
				else -> reason ?: "$total Teile"
			}

		/** Bild der Reihe (ohne Anmeldung), null wenn der Server keins hat. */
		fun imageUrl(baseUrl: String?): String? =
			if (hasBackdrop && baseUrl != null) "${baseUrl.trimEnd('/')}/LocalRecs/MovieSeries/${java.net.URLEncoder.encode(id, "UTF-8")}/Images/Backdrop" else null
	}

	data class SeriesDetail(val series: MovieSeries, val parts: List<BaseItemDto>)

	private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

	/** Eintrag aus „Demnächst“: Folge (Klick öffnet die Serie) oder Film aus Radarr (Klick zeigt Titel und Text). */
	data class Upcoming(
		val title: String,
		val subtitle: String?,
		val series: BaseItemDto?,
		val year: Int? = null,
		val overview: String? = null,
		val imageUrl: String? = null,
	)

	/** Antwort von GET /LocalRecs/Upcoming (testbar); Bildadressen sind relativ zum Server. */
	fun parseUpcoming(baseUrl: String?, text: String): List<Upcoming> {
		val base = baseUrl?.trimEnd('/')
		fun abs(path: String?) = if (path.isNullOrBlank() || base == null) null else "$base/${path.trimStart('/')}"
		return ((json.parseToJsonElement(text) as? JsonObject)?.get("Items") as? JsonArray).orEmpty().mapNotNull { entry ->
			val obj = entry as? JsonObject ?: return@mapNotNull null
			val title = obj.text("Title") ?: return@mapNotNull null
			when (obj.text("Kind")) {
				"Episode" -> {
					val series = (obj["Series"] as? JsonObject)?.let { runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), it) }.getOrNull() } ?: return@mapNotNull null
					Upcoming(title, obj.text("Subtitle"), series)
				}
				"Movie" -> {
					val movie = obj["Movie"] as? JsonObject
					Upcoming(
						title = title,
						subtitle = obj.text("Subtitle"),
						series = null,
						year = movie?.text("Year")?.toIntOrNull(),
						overview = movie?.text("Overview"),
						imageUrl = abs(movie?.text("BackdropUrl")) ?: abs(movie?.text("PosterUrl")),
					)
				}
				else -> null
			}
		}
	}

	/** Zeile „Demnächst“ (Sonarr/Radarr über das Plugin ab 12.48). */
	suspend fun upcomingRow(title: String): HomeFragmentRow? = withContext(Dispatchers.IO) {
		if (!config().upcoming) return@withContext null
		val entries = runCatching {
			parseUpcoming(baseUrl(), TvClientBehavior.call("GET", "/LocalRecs/Upcoming?utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}").second)
		}.onFailure { Timber.w(it, "Demnächst nicht verfügbar") }.getOrDefault(emptyList())
		if (entries.isEmpty()) null else HomeFragmentUpcomingRow(title, entries)
	}

	private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

	private fun baseUrl() = org.koin.java.KoinJavaComponent.get<org.jellyfin.sdk.api.client.ApiClient>(org.jellyfin.sdk.api.client.ApiClient::class.java).baseUrl

	suspend fun config(): Config = withContext(Dispatchers.IO) {
		val base = baseUrl()
		if (configFor == base) return@withContext config
		config = runCatching {
			val obj = json.parseToJsonElement(TvClientBehavior.call("GET", "/LocalRecs/Web/Config").second) as JsonObject
			Config(
				movieSeries = (obj["MovieSeries"] as? JsonPrimitive)?.booleanOrNull == true,
				movieSeriesTitle = (obj["MovieSeriesTitle"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
				seasonal = (obj["Seasonal"] as? JsonPrimitive)?.booleanOrNull == true,
				moments = (obj["Moments"] as? JsonPrimitive)?.booleanOrNull == true,
				upcoming = (obj["Upcoming"] as? JsonPrimitive)?.booleanOrNull == true,
				moviesTitle = obj.text("MoviesTitle"),
				seriesTitle = obj.text("SeriesTitle"),
				liveTvTitle = obj.text("LiveTvTitle"),
				liveTvRow = (obj["LiveTvRow"] as? JsonPrimitive)?.booleanOrNull != false,
			)
		}.getOrDefault(Config())
		configFor = base
		config
	}

	/** Antwort von GET /LocalRecs/MovieSeries (testbar). */
	fun parseMovieSeries(text: String): List<MovieSeries> =
		((json.parseToJsonElement(text) as? JsonObject)?.get("Items") as? JsonArray).orEmpty().mapNotNull { entry ->
			val obj = entry as? JsonObject ?: return@mapNotNull null
			seriesOf(obj)
		}

	private fun seriesOf(obj: JsonObject) = MovieSeries(
		id = (obj["Id"] as? JsonPrimitive)?.content.orEmpty(),
		name = (obj["Name"] as? JsonPrimitive)?.content.orEmpty(),
		reason = (obj["Reason"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
		next = (obj["Next"] as? JsonObject)?.let { runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), it) }.getOrNull() },
		total = obj.int("Total"),
		watched = obj.int("Watched"),
		nextIndex = obj.int("NextIndex"),
		hasBackdrop = (obj["HasBackdrop"] as? JsonPrimitive)?.booleanOrNull == true,
	)

	/** Reihen-Seite: GET /LocalRecs/MovieSeries/{Id} (testbar über [parseDetail]). */
	suspend fun seriesDetail(id: String): SeriesDetail? = withContext(Dispatchers.IO) {
		runCatching {
			val (code, body) = TvClientBehavior.call("GET", "/LocalRecs/MovieSeries/${java.net.URLEncoder.encode(id, "UTF-8")}")
			if (code == 200) parseDetail(body) else null
		}.onFailure { Timber.w(it, "Filmreihe nicht verfügbar") }.getOrNull()
	}

	fun parseDetail(text: String): SeriesDetail {
		val obj = json.parseToJsonElement(text) as JsonObject
		return SeriesDetail(seriesOf(obj), parseParts(text).second)
	}

	/** Teile einer Reihe aus GET /LocalRecs/MovieSeries/ByItem/{id} bzw. /{id} (testbar). */
	fun parseParts(text: String): Pair<String, List<BaseItemDto>> {
		val obj = json.parseToJsonElement(text) as JsonObject
		val parts = (obj["Parts"] as? JsonArray).orEmpty().mapNotNull { part ->
			(part as? JsonObject)?.let { runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), it) }.getOrNull() }
		}
		return (obj["Name"] as? JsonPrimitive)?.content.orEmpty() to parts
	}

	/** Zeile „Filmreihen für dich“: je Reihe der Teil zum Weiterschauen. */
	suspend fun movieSeriesRow(defaultTitle: String): HomeFragmentRow? = withContext(Dispatchers.IO) {
		val cfg = config()
		if (!cfg.movieSeries) return@withContext null
		val series = runCatching { parseMovieSeries(TvClientBehavior.call("GET", "/LocalRecs/MovieSeries?limit=12").second) }
			.onFailure { Timber.w(it, "Filmreihen nicht verfügbar") }.getOrDefault(emptyList())
		val cards = series.filter { it.next != null }
		if (cards.isEmpty()) return@withContext null
		reasons.value = reasons.value + cards.mapNotNull { s -> s.next?.id?.let { key(it) to s.caption } }
		HomeFragmentMovieSeriesRow(cfg.movieSeriesTitle ?: defaultTitle, cards)
	}

	data class Moment(val key: String, val title: String, val items: List<BaseItemDto>, val reasons: Map<String, String>)

	/** Antwort von GET /LocalRecs/Moments (testbar): Zeilen in Anzeigereihenfolge. */
	fun parseMoments(text: String): List<Moment> =
		((json.parseToJsonElement(text) as? JsonObject)?.get("Rows") as? JsonArray).orEmpty().mapNotNull { entry ->
			val obj = entry as? JsonObject ?: return@mapNotNull null
			val items = (obj["Items"] as? JsonArray).orEmpty().mapNotNull { item ->
				(item as? JsonObject)?.let { runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), it) }.getOrNull() }
			}
			val title = (obj["Title"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
			if (items.isEmpty()) return@mapNotNull null
			val reasons = (obj["Reasons"] as? JsonObject).orEmpty().mapNotNull { (id, value) ->
				(value as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { id.replace("-", "").lowercase() to it }
			}.toMap()
			Moment((obj["Key"] as? JsonPrimitive)?.content.orEmpty(), title, items, reasons)
		}

	/**
	 * Zeitabhängige Zeilen (Plugin ab 12.46): „Läuft heute im TV – hier ohne Werbung“, „Neu diese Woche“,
	 * „Vor dem Kinostart“, „Heute vor X Jahren“, „Vor einem Jahr …“ – Auswahl und Reihenfolge vom Server.
	 */
	suspend fun momentRows(): List<HomeFragmentRow> = withContext(Dispatchers.IO) {
		if (!config().moments) return@withContext emptyList()
		val moments = runCatching {
			parseMoments(TvClientBehavior.call("GET", "/LocalRecs/Moments?utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}").second)
		}.onFailure { Timber.w(it, "Momente nicht verfügbar") }.getOrDefault(emptyList())
		reasons.value = reasons.value + moments.flatMap { it.reasons.entries }.associate { it.key to it.value }
		moments.map { StaticRow(it.title, it.items) }
	}

	/** Saison-Zeile („Weihnachtsfilme“) – nur wenn der Server sie gerade aktiv meldet. */
	suspend fun seasonalRow(): HomeFragmentRow? = withContext(Dispatchers.IO) {
		if (!config().seasonal) return@withContext null
		val offset = TvClientBehavior.utcOffsetMinutes()
		val (active, title) = runCatching {
			val obj = json.parseToJsonElement(TvClientBehavior.call("GET", "/LocalRecs/Seasonal?utcOffsetMinutes=$offset").second) as JsonObject
			((obj["Active"] as? JsonPrimitive)?.booleanOrNull == true) to (obj["Title"] as? JsonPrimitive)?.content
		}.getOrDefault(false to null)
		if (!active || title.isNullOrBlank()) return@withContext null
		HomeFragmentLocalRecommendationsRow(title, SeasonalRequest(offset))
	}

	private class SeasonalRequest(private val offset: Int) : ExternalItemsRequest {
		override val path get() = "/LocalRecs/Seasonal"
		override val queryParameters get() = mapOf<String, Any?>("utcOffsetMinutes" to offset)
	}

	/** Zeile mit fest vorgegebenen Titeln (in der Reihenfolge des Servers). */
	private class StaticRow(private val title: String, private val items: List<BaseItemDto>) : HomeFragmentRow {
		override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
			val rowAdapter = ItemRowAdapter(context, items, cardPresenter, rowsAdapter, QueryType.StaticItems)
			val row = ListRow(HeaderItem(title), rowAdapter)
			rowAdapter.setRow(row)
			rowAdapter.Retrieve()
			rowsAdapter.add(row)
		}
	}

	/** Detailseite eines Films: Teile der Reihe laden und – wenn es welche gibt – als Zeile übergeben. */
	@JvmStatic
	fun loadParts(lifecycle: Lifecycle, itemId: UUID, onParts: (List<BaseItemDto>) -> Unit) {
		lifecycle.coroutineScope.launch {
			val parts = withContext(Dispatchers.IO) {
				if (!config().movieSeries) return@withContext emptyList()
				runCatching {
					val (code, body) = TvClientBehavior.call("GET", "/LocalRecs/MovieSeries/ByItem/${key(itemId)}")
					if (code == 200) parseParts(body).second else emptyList()
				}.getOrDefault(emptyList())
			}
			if (parts.size > 1) onParts(parts)
		}
	}

	fun key(id: UUID): String = id.toString().replace("-", "").lowercase()
}
