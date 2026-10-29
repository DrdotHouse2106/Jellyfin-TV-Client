package org.jellyfin.androidtv.tvclient

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.serializer.toUUID
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
		/** Entdecken-Reihen (Kinostarts, Neu dieses Jahr; Plugin ab 12.54). */
		val discover: Boolean = false,
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
		val episode: Episode? = null,
	)

	/** Kommende Folge (Plugin ab 12.49): Staffel/Folge, Termin (UTC, ISO 8601), Sender, Beschreibung, weitere neue Folgen. */
	data class Episode(
		val season: Int? = null,
		val number: Int? = null,
		val title: String? = null,
		val date: String? = null,
		val network: String? = null,
		val overview: String? = null,
		val more: Int = 0,
		/** Uhrzeit vom Server in der Gerätezone (ab 12.52.2), z. B. „15:30 Uhr MEZ“. */
		val localTime: String? = null,
		/** „Danach: S03E06 am So. 01.11.“ (ab 12.52.2, bei weiteren neuen Folgen). */
		val nextAfter: String? = null,
	) {
		/** „S02E05 · Folgentitel“. */
		val line: String
			get() = listOfNotNull(
				if (season != null && number != null) "S%02dE%02d".format(season, number) else null,
				title,
			).joinToString(" · ")

		/**
		 * „Samstag, 24.10.2026 · 21:00 Uhr (MESZ) · Paramount+“ – der Termin kommt in UTC (Sonarr airDateUtc) und
		 * wird in die Zeitzone des Geräts umgerechnet; die Zone steht dabei, damit klar ist, welche Zeit gemeint ist.
		 */
		fun whenText(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
			val local = date?.let { parseUtc(it) }?.atZone(zone)
			return listOfNotNull(
				local?.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", java.util.Locale.GERMAN)),
				// Uhrzeit bevorzugt vom Server (in der Gerätezone berechnet), sonst selbst umgerechnet
				localTime?.let { if (it.contains("Uhr")) it else "$it Uhr" }
					?: local?.let { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm 'Uhr'")) + " (" + zoneLabel(it) + ")" },
				network,
			).joinToString(" · ")
		}
	}

	/** ISO-Zeitpunkt mit Zone („…Z“, „…+02:00“); ohne Angabe gilt UTC. */
	fun parseUtc(text: String): java.time.Instant? =
		runCatching { java.time.OffsetDateTime.parse(text).toInstant() }.getOrNull()
			?: runCatching { java.time.LocalDateTime.parse(text).toInstant(java.time.ZoneOffset.UTC) }.getOrNull()

	/** „MEZ“/„MESZ“ für mitteleuropäische Zeit, sonst die Abweichung von UTC („UTC+9“). */
	fun zoneLabel(time: java.time.ZonedDateTime): String {
		val central = setOf("Europe/Berlin", "Europe/Vienna", "Europe/Zurich", "Europe/Amsterdam", "Europe/Brussels", "Europe/Luxembourg", "Europe/Paris", "Europe/Rome", "Europe/Madrid", "Europe/Copenhagen", "Europe/Oslo", "Europe/Stockholm", "Europe/Prague", "Europe/Warsaw", "Europe/Budapest", "CET")
		if (time.zone.id in central) return if (time.zone.rules.isDaylightSavings(time.toInstant())) "MESZ" else "MEZ"
		val seconds = time.offset.totalSeconds
		if (seconds == 0) return "UTC"
		val hours = seconds / 3600
		val minutes = Math.abs(seconds % 3600) / 60
		return "UTC" + (if (seconds > 0) "+" else "-") + Math.abs(hours) + (if (minutes > 0) ":%02d".format(minutes) else "")
	}

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
					Upcoming(
						title = title,
						subtitle = obj.text("Subtitle"),
						series = series,
						episode = Episode(
							season = obj.text("SeasonNumber")?.toIntOrNull(),
							number = obj.text("EpisodeNumber")?.toIntOrNull(),
							title = obj.text("EpisodeTitle"),
							date = obj.text("Date"),
							network = obj.text("Network"),
							overview = obj.text("EpisodeOverview"),
							more = obj.text("MoreEpisodes")?.toIntOrNull() ?: 0,
							localTime = obj.text("LocalTime"),
							nextAfter = (obj["NextAfter"] as? JsonObject)?.text("Text"),
						),
					)
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
			parseUpcoming(baseUrl(), TvClientBehavior.call("GET", "/LocalRecs/Upcoming?utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}${TvClientBehavior.timeZoneParameter()}").second)
		}.onFailure { Timber.w(it, "Demnächst nicht verfügbar") }.getOrDefault(emptyList())
		if (entries.isEmpty()) null else HomeFragmentUpcomingRow(title, entries)
	}

	/** Eintrag einer Entdecken-Reihe (TMDb): Kinostart bzw. Neuheit, ggf. schon in der Bibliothek. */
	data class Discover(
		val tmdbId: Int,
		val mediaType: String,
		val title: String,
		val year: Int? = null,
		val overview: String? = null,
		val imageUrl: String? = null,
		val releaseText: String? = null,
		val genres: List<String> = emptyList(),
		val officialRating: String? = null,
		val runtimeMinutes: Int? = null,
		val providers: List<String> = emptyList(),
		val trailerKey: String? = null,
		val inLibrary: Boolean = false,
		val jellyfinId: UUID? = null,
	) {
		/** „Genres · FSK 12 · 1 Std. 50 Min.“ */
		val facts: String
			get() = listOfNotNull(
				genres.take(3).joinToString(", ").takeIf { it.isNotEmpty() },
				ParentalRating.display(officialRating),
				runtimeMinutes?.takeIf { it > 0 }?.let { if (it >= 60) "${it / 60} Std. ${it % 60} Min." else "$it Min." },
			).joinToString(" · ")
	}

	data class DiscoverRow(val key: String, val title: String, val items: List<Discover>)

	/** Antwort von GET /LocalRecs/Discover (testbar); Bildadressen können relativ zum Server sein. */
	fun parseDiscover(baseUrl: String?, text: String): List<DiscoverRow> {
		val base = baseUrl?.trimEnd('/')
		fun abs(path: String?) = when {
			path.isNullOrBlank() -> null
			path.startsWith("http") -> path
			base == null -> null
			else -> "$base/${path.trimStart('/')}"
		}
		fun JsonObject.strings(key: String) = (this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content?.takeIf { s -> s.isNotBlank() } }
		return ((json.parseToJsonElement(text) as? JsonObject)?.get("Rows") as? JsonArray).orEmpty().mapNotNull { rowEntry ->
			val row = rowEntry as? JsonObject ?: return@mapNotNull null
			val title = row.text("Title") ?: return@mapNotNull null
			val items = (row["Items"] as? JsonArray).orEmpty().mapNotNull { entry ->
				val obj = entry as? JsonObject ?: return@mapNotNull null
				val itemTitle = obj.text("Title") ?: return@mapNotNull null
				Discover(
					tmdbId = obj.text("TmdbId")?.toIntOrNull() ?: return@mapNotNull null,
					mediaType = obj.text("MediaType") ?: "movie",
					title = itemTitle,
					year = obj.text("Year")?.toIntOrNull(),
					overview = obj.text("Overview"),
					imageUrl = abs(obj.text("BackdropUrl")) ?: abs(obj.text("PosterUrl")),
					releaseText = obj.text("ReleaseText"),
					genres = obj.strings("Genres"),
					officialRating = obj.text("OfficialRating"),
					runtimeMinutes = obj.text("RuntimeMinutes")?.toIntOrNull(),
					providers = obj.strings("Providers"),
					trailerKey = obj.text("TrailerKey"),
					inLibrary = (obj["InLibrary"] as? JsonPrimitive)?.booleanOrNull == true,
					jellyfinId = obj.text("JellyfinId")?.let { runCatching { it.toUUID() }.getOrNull() },
				)
			}
			if (items.isEmpty()) null else DiscoverRow(row.text("Key").orEmpty(), title, items)
		}
	}

	/** Entdecken-Reihen „Demnächst im Kino“, „Neu dieses Jahr – Filme/Serien“ (Plugin ab 12.54), Reihenfolge vom Server. */
	suspend fun discoverRows(): List<HomeFragmentRow> = withContext(Dispatchers.IO) {
		if (!config().discover) return@withContext emptyList()
		runCatching {
			parseDiscover(baseUrl(), TvClientBehavior.call("GET", "/LocalRecs/Discover").second)
		}.onFailure { Timber.w(it, "Entdecken nicht verfügbar") }.getOrDefault(emptyList())
			.map { HomeFragmentDiscoverRow(it.title, it.items) }
	}

	private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

	private fun baseUrl() = org.koin.java.KoinJavaComponent.get<org.jellyfin.sdk.api.client.ApiClient>(org.jellyfin.sdk.api.client.ApiClient::class.java).baseUrl

	suspend fun config(): Config = withContext(Dispatchers.IO) {
		val base = baseUrl()
		if (configFor == base) return@withContext config
		// Nur eine erfolgreiche Antwort merken – sonst blieben nach einem Aussetzer beim Start alle Zeilen bis zum Neustart aus
		runCatching {
			val (code, body) = TvClientBehavior.call("GET", "/LocalRecs/Web/Config")
			// Plugin fehlt oder ist zu alt: das ist eine Antwort, kein Aussetzer
			if (code == 404) return@runCatching Config()
			val obj = json.parseToJsonElement(body) as JsonObject
			Config(
				movieSeries = (obj["MovieSeries"] as? JsonPrimitive)?.booleanOrNull == true,
				movieSeriesTitle = (obj["MovieSeriesTitle"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
				seasonal = (obj["Seasonal"] as? JsonPrimitive)?.booleanOrNull != false,
				moments = (obj["Moments"] as? JsonPrimitive)?.booleanOrNull == true,
				upcoming = (obj["Upcoming"] as? JsonPrimitive)?.booleanOrNull == true,
				discover = (obj["Discover"] as? JsonPrimitive)?.booleanOrNull == true,
				moviesTitle = obj.text("MoviesTitle"),
				seriesTitle = obj.text("SeriesTitle"),
				liveTvTitle = obj.text("LiveTvTitle"),
				liveTvRow = (obj["LiveTvRow"] as? JsonPrimitive)?.booleanOrNull != false,
			)
		}.onSuccess {
			config = it
			configFor = base
		}.onFailure { Timber.w(it, "LocalRecs-Einstellungen nicht verfügbar") }.getOrDefault(Config())
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
		// Atomar ergänzen – mehrere Reihen laden gleichzeitig
		reasons.update { it + cards.mapNotNull { s -> s.next?.id?.let { key(it) to s.caption } } }
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
		reasons.update { it + moments.flatMap { m -> m.reasons.entries }.associate { e -> e.key to e.value } }
		// Begründung je Karte darunter (z. B. „vor 20 Jahren“), wie im Web
		moments.map { StaticRow(it.title, it.items, it.reasons) }
	}

	data class Seasonal(val title: String, val items: List<BaseItemDto>)

	/** Antwort von GET /LocalRecs/Seasonal (testbar); null, wenn gerade keine Saison aktiv ist. */
	fun parseSeasonal(text: String): Seasonal? {
		val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
		if ((obj["Active"] as? JsonPrimitive)?.booleanOrNull != true) return null
		val title = obj.text("Title") ?: return null
		val items = (obj["Items"] as? JsonArray).orEmpty().mapNotNull { item ->
			(item as? JsonObject)?.let { runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), it) }.getOrNull() }
		}
		return if (items.isEmpty()) null else Seasonal(title, items)
	}

	/**
	 * Saison-Zeile (z. B. „Halloween“, „Weihnachtsfilme“) – nur wenn der Server sie gerade aktiv meldet.
	 * Die Titel kommen gleich mit (die Antwort hat kein TotalRecordCount, als Abfrage-Ergebnis gelesen blieb die Zeile leer).
	 */
	suspend fun seasonalRow(): HomeFragmentRow? = withContext(Dispatchers.IO) {
		if (!config().seasonal) return@withContext null
		val seasonal = runCatching {
			parseSeasonal(TvClientBehavior.call("GET", "/LocalRecs/Seasonal?utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}").second)
		}.onFailure { Timber.w(it, "Saison-Zeile nicht verfügbar") }.getOrNull()
		seasonal?.let { StaticRow(it.title, it.items) }
	}

	/** Zeile mit fest vorgegebenen Titeln (in der Reihenfolge des Servers). */
	private class StaticRow(
		private val title: String,
		private val items: List<BaseItemDto>,
		private val captions: Map<String, String> = emptyMap(),
	) : HomeFragmentRow {
		override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
			val presenter = if (captions.isEmpty()) cardPresenter else cardPresenter.withCaptions(captions)
			val rowAdapter = ItemRowAdapter(context, items, presenter, rowsAdapter, QueryType.StaticItems)
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
