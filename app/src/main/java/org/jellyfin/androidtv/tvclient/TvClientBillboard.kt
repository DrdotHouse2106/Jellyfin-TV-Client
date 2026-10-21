package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.UUID

/**
 * Jellyfin-TV-Client: Banner-Auswahl vom Server (Plugin Local Recommendations ab 12.41): täglich wechselnd,
 * gemischt mit gerade Passendem (Weiterschauen, neue Folgen). „Später“ legt einen Titel für einige Wochen
 * aus dem Banner, ohne als Ablehnung zu zählen.
 */
object TvClientBillboard {
	private val json = Json { ignoreUnknownKeys = true }
	private var availableFor: String? = null
	private var available = false

	/** Badges der Banner-Titel („Weiterschauen“, „Neue Folgen“ …): Id ohne Bindestriche → Text. */
	val reasons = MutableStateFlow<Map<String, String>>(emptyMap())

	data class Selection(val ids: List<UUID>, val reasons: Map<String, String>)
	data class Later(val id: UUID, val name: String, val until: String?)

	private fun baseUrl() = KoinJavaComponent.get<ApiClient>(ApiClient::class.java).baseUrl

	/** Bietet der Server die Banner-Auswahl an? (GET /LocalRecs/Web/Config → „Billboard“: true) */
	suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
		val base = baseUrl()
		if (availableFor == base) return@withContext available
		available = runCatching {
			val config = json.parseToJsonElement(TvClientBehavior.call("GET", "/LocalRecs/Web/Config").second) as? JsonObject
			(config?.get("Billboard") as? JsonPrimitive)?.booleanOrNull == true
		}.getOrDefault(false)
		availableFor = base
		available
	}

	/** Auswahl für heute; null ohne Plugin oder bei Fehler (dann wählt die App selbst). */
	suspend fun load(limit: Int): Selection? = withContext(Dispatchers.IO) {
		if (!isAvailable()) return@withContext null
		runCatching {
			val (code, body) = TvClientBehavior.call("GET", "/LocalRecs/Billboard?limit=$limit&utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}")
			if (code !in 200..299) return@runCatching null
			parse(body)
		}.onFailure { Timber.w(it, "Banner-Auswahl vom Server nicht verfügbar") }.getOrNull()
			?.also { reasons.value = it.reasons }
	}

	fun parse(text: String): Selection {
		val obj = json.parseToJsonElement(text) as JsonObject
		val ids = (obj["Items"] as? JsonArray).orEmpty().mapNotNull { item ->
			((item as? JsonObject)?.get("Id") as? JsonPrimitive)?.content?.let { runCatching { uuid(it) }.getOrNull() }
		}
		val reasons = (obj["Reasons"] as? JsonObject).orEmpty().mapNotNull { (id, value) ->
			(value as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() }?.let { id.replace("-", "").lowercase() to it }
		}.toMap()
		return Selection(ids, reasons)
	}

	/** Titel für [days] Tage aus dem Banner nehmen. */
	suspend fun later(id: UUID, days: Int = 21): Boolean = withContext(Dispatchers.IO) {
		runCatching {
			TvClientBehavior.call("POST", "/LocalRecs/Billboard/Later", buildJsonObject { put("ItemId", id.toString()); put("Days", days) }.toString()).first in 200..299
		}.getOrDefault(false)
	}

	/** Weggelegte Titel („Wieder zeigen“ in den Einstellungen). */
	suspend fun laterList(): List<Later>? = withContext(Dispatchers.IO) {
		if (!isAvailable()) return@withContext null
		runCatching {
			(json.parseToJsonElement(TvClientBehavior.call("GET", "/LocalRecs/Billboard/Later").second) as? JsonArray).orEmpty().mapNotNull { entry ->
				val obj = entry as? JsonObject ?: return@mapNotNull null
				val id = (obj["ItemId"] as? JsonPrimitive)?.content?.let { runCatching { uuid(it) }.getOrNull() } ?: return@mapNotNull null
				Later(id, (obj["Name"] as? JsonPrimitive)?.content.orEmpty(), (obj["Until"] as? JsonPrimitive)?.content)
			}
		}.getOrNull()
	}

	/** Titel wieder im Banner zeigen. */
	suspend fun showAgain(id: UUID): Boolean = withContext(Dispatchers.IO) {
		runCatching { TvClientBehavior.call("DELETE", "/LocalRecs/Billboard/Later/${id.toString().replace("-", "")}").first in 200..299 }.getOrDefault(false)
	}

	/** Id mit oder ohne Bindestriche. */
	fun uuid(text: String): UUID {
		val hex = text.replace("-", "")
		require(hex.length == 32)
		return UUID.fromString("${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}")
	}
}
