package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.stringPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Jellyfin-TV-Client: Filme und Serien bei Jellyseerr (bzw. Overseerr) suchen und anfragen.
 *
 * Bevorzugt über das Jellyfin-Plugin „Jellyfin Enhanced“: Dann braucht die App nur die normale
 * Jellyfin-Anmeldung, der Jellyseerr-Schlüssel bleibt auf dem Server und die Anfrage läuft unter dem
 * verknüpften Benutzer. Der Knopf erscheint nur, wenn das Plugin eingerichtet und der Benutzer
 * verknüpft ist. Ohne Plugin geht es optional direkt mit Jellyseerr-Adresse und API-Schlüssel.
 */
object Jellyseerr {
	val enabledPreference = booleanPreference("pref_tvclient_jellyseerr_button", true)
	val urlPreference = stringPreference("pref_tvclient_jellyseerr_url", "")
	val apiKeyPreference = stringPreference("pref_tvclient_jellyseerr_api_key", "")

	private const val TIMEOUT_MS = 10_000
	private const val TMDB_IMAGES = "https://image.tmdb.org/t/p/w342"
	private const val TMDB_BACKDROPS = "https://image.tmdb.org/t/p/w1280"
	private const val TMDB_PROFILES = "https://image.tmdb.org/t/p/w185"
	private const val MAX_CAST = 15
	private const val PLUGIN_PATH = "/JellyfinEnhanced/jellyseerr"
	private val json = Json { ignoreUnknownKeys = true }

	enum class Status { NONE, REQUESTED, PARTIAL, AVAILABLE }

	data class Media(
		val id: Int,
		val type: String,
		val title: String,
		val year: String?,
		val overview: String?,
		val posterUrl: String?,
		val status: Status,
		/** Jellyfin-Eintrag, falls schon in der Bibliothek. */
		val jellyfinId: UUID?,
		/** Erscheinungsdatum (JJJJ-MM-TT), zum Sortieren nach Aktualität. */
		val date: String? = null,
	)

	/** Wohin die Anfragen gehen. */
	sealed interface Backend {
		/** Über das Jellyfin-Plugin „Jellyfin Enhanced“. */
		data class Plugin(val baseUrl: String, val token: String) : Backend

		/** Direkt an Jellyseerr mit API-Schlüssel. */
		data class Direct(val baseUrl: String, val apiKey: String) : Backend
	}

	class RequestException(val code: Int) : Exception("Jellyseerr antwortet mit $code")

	@Volatile
	private var backend: Backend? = null

	private fun prefs() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)

	/** Adresse vereinheitlichen: ohne Schema wird https angenommen, ohne Schrägstrich am Ende. */
	fun normalizeUrl(input: String): String {
		val trimmed = input.trim().trimEnd('/')
		if (trimmed.isEmpty()) return ""
		return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) trimmed else "https://$trimmed"
	}

	/**
	 * Prüft, ob Anfragen möglich sind (Plugin eingerichtet und Benutzer verknüpft, sonst eigene
	 * Adresse und Schlüssel). Gibt null zurück, wenn der Knopf nicht erscheinen soll.
	 */
	suspend fun detect(api: ApiClient): Backend? {
		if (!prefs()[enabledPreference]) return null.also { backend = null }

		val baseUrl = api.baseUrl?.trimEnd('/')
		val token = api.accessToken
		if (baseUrl != null && token != null) {
			val plugin = Backend.Plugin("$baseUrl$PLUGIN_PATH", token)
			val usable = runCatching {
				val status = json.parseToJsonElement(call(plugin, "/status")).jsonObject
				if (status["active"]?.jsonPrimitive?.booleanOrNull != true) return@runCatching false
				val user = json.parseToJsonElement(call(plugin, "/user-status")).jsonObject
				user["active"]?.jsonPrimitive?.booleanOrNull == true && user["userFound"]?.jsonPrimitive?.booleanOrNull == true
			}.onFailure { Timber.i("Jellyseerr über Jellyfin Enhanced nicht verfügbar: %s", it.message) }.getOrDefault(false)
			if (usable) return plugin.also { backend = it }
		}

		val url = normalizeUrl(prefs()[urlPreference])
		val key = prefs()[apiKeyPreference].trim()
		return (if (url.isNotEmpty() && key.isNotEmpty()) Backend.Direct("$url/api/v1", key) else null).also { backend = it }
	}

	private fun current(): Backend = backend ?: throw IllegalStateException("Jellyseerr nicht eingerichtet")

	val supportsTrending: Boolean get() = backend is Backend.Direct

	private suspend fun call(target: Backend, path: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
		val base = when (target) {
			is Backend.Plugin -> target.baseUrl
			is Backend.Direct -> target.baseUrl
		}
		val connection = URL(base + path).openConnection() as HttpURLConnection
		try {
			connection.requestMethod = method
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			when (target) {
				// Dieser Server akzeptiert nur den Authorization-Header, nicht X-Emby-Token
				is Backend.Plugin -> connection.setRequestProperty("Authorization", "MediaBrowser Token=\"${target.token}\"")
				is Backend.Direct -> connection.setRequestProperty("X-Api-Key", target.apiKey)
			}
			connection.setRequestProperty("Accept", "application/json")
			if (body != null) {
				connection.doOutput = true
				connection.setRequestProperty("Content-Type", "application/json")
				connection.outputStream.use { it.write(body.toByteArray()) }
			}
			val code = connection.responseCode
			val stream = if (code in 200..299) connection.inputStream else connection.errorStream
			val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
			if (code !in 200..299) throw RequestException(code)
			text
		} finally {
			connection.disconnect()
		}
	}

	private fun JsonObject.string(key: String): String? =
		(this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

	/** Jellyseerr-Status: 2 angefragt, 3 in Bearbeitung, 4 teilweise da, 5 verfügbar. */
	fun statusOf(code: Int?): Status = when (code) {
		5 -> Status.AVAILABLE
		4 -> Status.PARTIAL
		2, 3 -> Status.REQUESTED
		else -> Status.NONE
	}

	/** Suchergebnisse (Plugin und Jellyseerr liefern dasselbe Format); Personen werden ausgelassen. */
	fun parseResults(text: String): List<Media> =
		parseMedia(json.parseToJsonElement(text).jsonObject["results"]?.jsonArray ?: emptyList<JsonElement>())

	private fun parseMedia(elements: List<JsonElement>): List<Media> =
		elements.mapNotNull { element ->
			val obj = element as? JsonObject ?: return@mapNotNull null
			val type = obj.string("mediaType") ?: return@mapNotNull null
			if (type != "movie" && type != "tv") return@mapNotNull null
			val date = obj.string("releaseDate") ?: obj.string("firstAirDate")
			val mediaInfo = obj["mediaInfo"] as? JsonObject
			Media(
				id = (obj["id"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null,
				type = type,
				title = obj.string("title") ?: obj.string("name") ?: return@mapNotNull null,
				year = date?.take(4)?.takeIf { it.length == 4 },
				overview = obj.string("overview"),
				posterUrl = obj.string("posterPath")?.let { TMDB_IMAGES + it },
				status = statusOf((mediaInfo?.get("status") as? JsonPrimitive)?.intOrNull),
				jellyfinId = mediaInfo?.string("jellyfinMediaId")?.toUUIDOrNull(),
				date = date,
			)
		}

	suspend fun search(query: String): List<Media> {
		// „Superman 2025“: Jahreszahl am Ende abtrennen und selbst danach filtern (Jellyseerr versteht sie nicht)
		val (text, year) = splitYear(query)
		val encoded = URLEncoder.encode(text.take(256), "UTF-8").replace("+", "%20")
		val results = parseResults(call(current(), "/search?query=$encoded&page=1&language=de"))
		return if (year == null) results else results.filter { it.year == year }.ifEmpty { results }
	}

	/** TMDB-Id einer Person, deren Name genau passt – nur wenn es genau einen solchen Treffer gibt. */
	suspend fun findPerson(name: String): Int? {
		val encoded = URLEncoder.encode(name.take(256), "UTF-8").replace("+", "%20")
		val results = json.parseToJsonElement(call(current(), "/search?query=$encoded&page=1&language=de")).jsonObject["results"]?.jsonArray.orEmpty()
		val matches = results.mapNotNull { it as? JsonObject }
			.filter { it.string("mediaType") == "person" && it.string("name").equals(name.trim(), ignoreCase = true) }
			.mapNotNull { (it["id"] as? JsonPrimitive)?.intOrNull }
			.distinct()
		return matches.singleOrNull()
	}

	/** Trennt eine vierstellige Jahreszahl (1900–2099) am Ende des Suchbegriffs ab. */
	fun splitYear(query: String): Pair<String, String?> {
		val match = Regex("^(.*\\S)\\s+\\(?((?:19|20)\\d{2})\\)?$").find(query.trim()) ?: return query.trim() to null
		return match.groupValues[1] to match.groupValues[2]
	}

	/** „Das könnte dir auch gefallen“ zu einem Titel (Jellyseerr-Empfehlungen, ohne bereits Gesehenes zu kennen). */
	suspend fun recommendations(type: String, id: Int): List<Media> =
		parseResults(call(current(), "/$type/$id/recommendations?page=1")).filter { it.id != id }

	/** Gerade beliebte Titel (nur direkt mit Jellyseerr; das Plugin bietet dafür keinen Endpunkt). */
	suspend fun trending(): List<Media> =
		if (supportsTrending) parseResults(call(current(), "/discover/trending?page=1&language=de")) else emptyList()

	/** Jellyseerr-Benutzer zum Jellyfin-Konto (nur direkt; das Plugin ordnet selbst zu). */
	private suspend fun userIdFor(target: Backend.Direct, jellyfinUserId: UUID?): Int? {
		if (jellyfinUserId == null) return null
		val wanted = jellyfinUserId.toString().replace("-", "")
		return runCatching {
			(json.parseToJsonElement(call(target, "/user?take=200")).jsonObject["results"]?.jsonArray ?: emptyList<JsonElement>())
				.map { it.jsonObject }
				.firstOrNull { it.string("jellyfinUserId")?.replace("-", "").equals(wanted, true) }
				?.get("id")?.jsonPrimitive?.intOrNull
		}.getOrNull()
	}

	/** Anfrage stellen; bei Serien alle Staffeln oder nur [seasons]. */
	suspend fun request(media: Media, jellyfinUserId: UUID?, seasons: List<Int>? = null) {
		val target = current()
		val userId = (target as? Backend.Direct)?.let { userIdFor(it, jellyfinUserId) }
		val body = buildJsonObject {
			put("mediaType", media.type)
			put("mediaId", media.id)
			if (media.type == "tv") {
				if (seasons.isNullOrEmpty()) put("seasons", "all")
				else put("seasons", kotlinx.serialization.json.JsonArray(seasons.map { JsonPrimitive(it) }))
			}
			if (userId != null) put("userId", userId)
		}
		call(target, "/request", method = "POST", body = body.toString())
	}

	data class Video(val key: String, val type: String, val name: String, val language: String?)

	/**
	 * Trailer auswählen (testbar): deutsche Videos zuerst (Sprache „de“ oder Name mit „deutsch“,
	 * „german“, „offiziell“, „Kinostart“), dabei Trailer vor Teaser; erst danach andere Sprachen.
	 */
	fun pickVideo(videos: List<Video>): Video? {
		fun Video.german() = language.equals("de", true) ||
			listOf("deutsch", "german", "offiziell", "kinostart").any { name.contains(it, true) }
		fun Video.rank() = when (type) {
			"Trailer" -> 0
			"Teaser" -> 1
			else -> 2
		}
		val usable = videos.filter { it.type == "Trailer" || it.type == "Teaser" }
		return usable.filter { it.german() }.minByOrNull { it.rank() } ?: usable.minByOrNull { it.rank() }
	}

	/**
	 * Kennzeichnung für nicht-deutsche Trailer (testbar). Im Zweifel lieber „EN“ als fälschlich deutsch:
	 * „OmU“ bei Untertiteln oder asiatischer Originalsprache, „EN“ bei Englisch bzw. unbekannt,
	 * sonst das Sprachkürzel.
	 */
	fun languageLabel(video: Video, originalLanguage: String?): String? {
		if (video.language.equals("de", true) || listOf("deutsch", "german", "offiziell", "kinostart").any { video.name.contains(it, true) }) return null
		val subtitled = Regex("(?i)\\b(subtitled|sub|subs|omu)\\b").containsMatchIn(video.name)
		if (subtitled || originalLanguage?.lowercase() in setOf("ja", "ko", "zh")) return "OmU"
		val language = video.language?.lowercase()
		return if (language == null || language == "en") "EN" else language.uppercase()
	}

	data class PersonDetails(
		val name: String,
		val imageUrl: String?,
		val biography: String?,
		val birthday: String?,
		val placeOfBirth: String?,
		val movies: List<Media>,
		val series: List<Media>,
	)

	/** Personenseite: Daten und Filmografie (neueste zuerst, doppelte zusammengefasst). */
	suspend fun person(id: Int): PersonDetails {
		val obj = json.parseToJsonElement(call(current(), "/person/$id?language=de")).jsonObject
		val credits = runCatching {
			val c = json.parseToJsonElement(call(current(), "/person/$id/combined_credits?language=de")).jsonObject
			parseMedia((c["cast"] as? kotlinx.serialization.json.JsonArray).orEmpty() + (c["crew"] as? kotlinx.serialization.json.JsonArray).orEmpty())
		}.getOrDefault(emptyList())
		val sorted = sortCredits(credits)
		return PersonDetails(
			name = obj.string("name").orEmpty(),
			imageUrl = obj.string("profilePath")?.let { TMDB_IMAGES + it },
			biography = obj.string("biography"),
			birthday = obj.string("birthday"),
			placeOfBirth = obj.string("placeOfBirth"),
			movies = sorted.filter { it.type == "movie" },
			series = sorted.filter { it.type == "tv" },
		)
	}

	/** Filmografie sortieren (testbar): neueste zuerst; ohne Datum oder Plakat ans Ende; doppelte weg. */
	fun sortCredits(credits: List<Media>): List<Media> = credits
		.distinctBy { "${it.type}-${it.id}" }
		.sortedWith(compareBy<Media> { it.date == null || it.posterUrl == null }.thenByDescending { it.date ?: "" })

	data class Person(val name: String, val role: String?, val imageUrl: String?, val id: Int? = null)
	data class Season(val number: Int, val name: String, val episodes: Int?, val status: Status)

	data class Details(
		val media: Media,
		val originalTitle: String?,
		val backdropUrl: String?,
		val runtimeMinutes: Int?,
		val seasonCount: Int?,
		val genres: List<String>,
		val rating: Double?,
		val ageRating: String?,
		val creators: List<String>,
		val cast: List<Person>,
		val seasons: List<Season>,
		val trailerKey: String?,
		/** Kennzeichnung, wenn der Trailer nicht deutsch ist („EN“, „OmU“ …), sonst null. */
		val trailerLabel: String? = null,
	)

	/** Alle Infos für die Detailseite (Film: /movie/{id}, Serie: /tv/{id}). */
	suspend fun details(type: String, id: Int): Details {
		val obj = json.parseToJsonElement(call(current(), "/$type/$id?language=de")).jsonObject
		val mediaInfo = obj["mediaInfo"] as? JsonObject
		val date = obj.string("releaseDate") ?: obj.string("firstAirDate")
		fun names(key: String) = (obj[key] as? kotlinx.serialization.json.JsonArray).orEmpty()
			.mapNotNull { (it as? JsonObject)?.string("name") }
		val credits = obj["credits"] as? JsonObject
		val crew = (credits?.get("crew") as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { it as? JsonObject }
		val cast = (credits?.get("cast") as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { it as? JsonObject }
			.take(MAX_CAST)
			.mapNotNull { person ->
				Person(
					id = (person["id"] as? JsonPrimitive)?.intOrNull,
					name = person.string("name") ?: return@mapNotNull null,
					role = person.string("character"),
					imageUrl = person.string("profilePath")?.let { TMDB_PROFILES + it },
				)
			}
		val ageRating = if (type == "movie") {
			((obj["releases"] as? JsonObject)?.get("results") as? kotlinx.serialization.json.JsonArray).orEmpty()
				.mapNotNull { it as? JsonObject }
				.firstOrNull { it.string("iso_3166_1") == "DE" }
				?.let { (it["release_dates"] as? kotlinx.serialization.json.JsonArray).orEmpty() }
				?.mapNotNull { (it as? JsonObject)?.string("certification") }
				?.firstOrNull()
		} else {
			((obj["contentRatings"] as? JsonObject)?.get("results") as? kotlinx.serialization.json.JsonArray).orEmpty()
				.mapNotNull { it as? JsonObject }
				.firstOrNull { it.string("iso_3166_1") == "DE" }
				?.string("rating")
		}
		val requestedSeasons = (mediaInfo?.get("seasons") as? kotlinx.serialization.json.JsonArray).orEmpty()
			.mapNotNull { it as? JsonObject }
			.associate { ((it["seasonNumber"] as? JsonPrimitive)?.intOrNull ?: -1) to statusOf((it["status"] as? JsonPrimitive)?.intOrNull) }
		val seasons = (obj["seasons"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { it as? JsonObject }
			.mapNotNull { season ->
				val number = (season["seasonNumber"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
				if (number == 0) return@mapNotNull null // Specials
				Season(number, season.string("name") ?: "Staffel $number", (season["episodeCount"] as? JsonPrimitive)?.intOrNull, requestedSeasons[number] ?: Status.NONE)
			}
		val trailerVideo = pickVideo((obj["relatedVideos"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { video ->
			val o = video as? JsonObject ?: return@mapNotNull null
			if (!o.string("site").equals("YouTube", true)) return@mapNotNull null
			Video(o.string("key") ?: return@mapNotNull null, o.string("type").orEmpty(), o.string("name").orEmpty(), o.string("iso_639_1") ?: o.string("language"))
		})
		val trailer = trailerVideo?.key

		return Details(
			media = Media(
				id = id,
				type = type,
				title = obj.string("title") ?: obj.string("name").orEmpty(),
				year = date?.take(4)?.takeIf { it.length == 4 },
				overview = obj.string("overview"),
				posterUrl = obj.string("posterPath")?.let { TMDB_IMAGES + it },
				status = statusOf((mediaInfo?.get("status") as? JsonPrimitive)?.intOrNull),
				jellyfinId = mediaInfo?.string("jellyfinMediaId")?.toUUIDOrNull(),
			),
			originalTitle = (obj.string("originalTitle") ?: obj.string("originalName"))
				?.takeIf { it != obj.string("title") && it != obj.string("name") },
			backdropUrl = obj.string("backdropPath")?.let { TMDB_BACKDROPS + it },
			runtimeMinutes = (obj["runtime"] as? JsonPrimitive)?.intOrNull
				?: (obj["episodeRunTime"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.intOrNull },
			seasonCount = (obj["numberOfSeasons"] as? JsonPrimitive)?.intOrNull,
			genres = names("genres"),
			rating = (obj["voteAverage"] as? JsonPrimitive)?.content?.toDoubleOrNull()?.takeIf { it > 0 },
			ageRating = ageRating?.takeIf { it.isNotBlank() },
			creators = if (type == "movie") crew.filter { it.string("job") == "Director" }.mapNotNull { it.string("name") }.distinct()
			else names("createdBy"),
			cast = cast,
			seasons = seasons,
			trailerKey = trailer,
			trailerLabel = trailerVideo?.let { languageLabel(it, obj.string("originalLanguage")) },
		)
	}
}
