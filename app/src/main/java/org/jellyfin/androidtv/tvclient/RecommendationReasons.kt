package org.jellyfin.androidtv.tvclient

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jellyfin.sdk.api.client.ApiClient
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Jellyfin-TV-Client: Begründungen zu den Empfehlungen („Weil du „…“ geschaut hast“, „Nächster Teil
 * nach „…““) vom Plugin „Local Recommendations“ (ab 12.9.0). Ohne Plugin oder ohne Eintrag: nichts.
 */
object RecommendationReasons {
	private const val TIMEOUT_MS = 8_000
	private val json = Json { ignoreUnknownKeys = true }

	/** Item-Id ohne Bindestriche (klein) → fertiger Text. */
	val reasons = MutableStateFlow<Map<String, String>>(emptyMap())

	/** Farbe der Begründung (wie die Hervorhebung „Neu“/„Passt zu dir“). */
	const val COLOR = 0xFF46D369.toInt()

	fun key(id: UUID): String = id.toString().replace("-", "").lowercase()

	fun reasonFor(id: UUID?): String? = id?.let { reasons.value[key(it)] }

	fun parse(text: String): Map<String, String> {
		val obj = json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
		return obj.mapNotNull { (id, value) ->
			val reason = (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
			reason?.let { id.replace("-", "").lowercase() to it }
		}.toMap()
	}

	/** Beim Öffnen der Startseite neu laden (parallel zu den Empfehlungen). */
	suspend fun refresh(api: ApiClient) = withContext(Dispatchers.IO) {
		val base = api.baseUrl?.trimEnd('/') ?: return@withContext
		val token = api.accessToken ?: return@withContext
		runCatching {
			val connection = URL("$base/LocalRecs/Recommendations/Reasons").openConnection() as HttpURLConnection
			try {
				connection.connectTimeout = TIMEOUT_MS
				connection.readTimeout = TIMEOUT_MS
				connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
				connection.setRequestProperty("Accept", "application/json")
				if (connection.responseCode in 200..299) parse(connection.inputStream.bufferedReader().use { it.readText() })
				else emptyMap()
			} finally {
				connection.disconnect()
			}
		}.onFailure { Timber.i("Begründungen nicht verfügbar: %s", it.message) }
			.getOrNull()?.let { reasons.value = it }
	}

	/** Detailseite: Begründung farbig vor die Genres setzen. */
	@JvmStatic
	fun withGenres(id: UUID?, genres: String?): CharSequence? {
		val reason = reasonFor(id) ?: return genres
		return SpannableStringBuilder().apply {
			append(reason, ForegroundColorSpan(COLOR), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
			if (!genres.isNullOrBlank()) append("  ·  ").append(genres)
		}
	}
}
