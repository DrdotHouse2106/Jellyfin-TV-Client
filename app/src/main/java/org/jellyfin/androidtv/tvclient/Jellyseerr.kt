package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.stringPreference
import org.koin.java.KoinJavaComponent
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Jellyfin-TV-Client: Filme und Serien bei Jellyseerr (bzw. Overseerr) suchen und anfragen.
 * Standardmäßig aus; braucht Adresse und API-Schlüssel (Jellyseerr → Einstellungen → Allgemein).
 */
object Jellyseerr {
	val enabledPreference = booleanPreference("pref_tvclient_jellyseerr", false)
	val urlPreference = stringPreference("pref_tvclient_jellyseerr_url", "")
	val apiKeyPreference = stringPreference("pref_tvclient_jellyseerr_api_key", "")

	private const val TIMEOUT_MS = 10_000
	private const val TMDB_IMAGES = "https://image.tmdb.org/t/p/w342"
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
	)

	private fun prefs() = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)

	fun isConfigured(): Boolean = prefs().let {
		it[enabledPreference] && it[urlPreference].isNotBlank() && it[apiKeyPreference].isNotBlank()
	}

	/** Adresse vereinheitlichen: ohne Schema wird https angenommen, ohne Schrägstrich am Ende. */
	fun normalizeUrl(input: String): String {
		val trimmed = input.trim().trimEnd('/')
		if (trimmed.isEmpty()) return ""
		return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) trimmed else "https://$trimmed"
	}

	private suspend fun call(path: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
		val base = normalizeUrl(prefs()[urlPreference])
		val connection = URL("$base/api/v1$path").openConnection() as HttpURLConnection
		try {
			connection.requestMethod = method
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("X-Api-Key", prefs()[apiKeyPreference].trim())
			connection.setRequestProperty("Accept", "application/json")
			if (body != null) {
				connection.doOutput = true
				connection.setRequestProperty("Content-Type", "application/json")
				connection.outputStream.use { it.write(body.toByteArray()) }
			}
			val code = connection.responseCode
			val stream = if (code in 200..299) connection.inputStream else connection.errorStream
			val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
			if (code !in 200..299) error("Jellyseerr antwortet mit $code")
			text
		} finally {
			connection.disconnect()
		}
	}

	private fun parseResults(text: String): List<Media> =
		json.parseToJsonElement(text).jsonObject["results"]?.jsonArray.orEmpty().mapNotNull { element ->
			val obj = element as? JsonObject ?: return@mapNotNull null
			val type = obj.string("mediaType") ?: return@mapNotNull null
			if (type != "movie" && type != "tv") return@mapNotNull null
			val date = obj.string("releaseDate") ?: obj.string("firstAirDate")
			Media(
				id = obj["id"]?.jsonPrimitive?.int ?: return@mapNotNull null,
				type = type,
				title = obj.string("title") ?: obj.string("name") ?: return@mapNotNull null,
				year = date?.take(4)?.takeIf { it.length == 4 },
				overview = obj.string("overview"),
				posterUrl = obj.string("posterPath")?.let { TMDB_IMAGES + it },
				status = statusOf((obj["mediaInfo"] as? JsonObject)?.get("status")?.jsonPrimitive?.intOrNull),
			)
		}

	/** Jellyseerr-Status: 2 angefragt, 3 in Bearbeitung, 4 teilweise da, 5 verfügbar. */
	fun statusOf(code: Int?): Status = when (code) {
		5 -> Status.AVAILABLE
		4 -> Status.PARTIAL
		2, 3 -> Status.REQUESTED
		else -> Status.NONE
	}

	private fun JsonObject.string(key: String): String? =
		(this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

	suspend fun search(query: String): List<Media> =
		parseResults(call("/search?query=${URLEncoder.encode(query, "UTF-8").replace("+", "%20")}&page=1&language=de"))

	suspend fun trending(): List<Media> = parseResults(call("/discover/trending?page=1&language=de"))

	/** Jellyseerr-Benutzer zum Jellyfin-Konto, damit die Anfrage unter dem richtigen Namen läuft. */
	private suspend fun userIdFor(jellyfinUserId: UUID?): Int? {
		if (jellyfinUserId == null) return null
		val wanted = jellyfinUserId.toString().replace("-", "")
		return runCatching {
			json.parseToJsonElement(call("/user?take=200")).jsonObject["results"]?.jsonArray.orEmpty()
				.map { it.jsonObject }
				.firstOrNull { it.string("jellyfinUserId")?.replace("-", "").equals(wanted, true) }
				?.get("id")?.jsonPrimitive?.intOrNull
		}.getOrNull()
	}

	suspend fun request(media: Media, jellyfinUserId: UUID?) {
		val userId = userIdFor(jellyfinUserId)
		val body = buildJsonObject {
			put("mediaType", media.type)
			put("mediaId", media.id)
			if (media.type == "tv") put("seasons", "all")
			if (userId != null) put("userId", userId)
		}
		call("/request", method = "POST", body = body.toString())
	}

}
