package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.UUID

/**
 * Jellyfin-TV-Client: Verhaltenssignale für das Plugin Local Recommendations (ab 12.36). Suchbegriffe und
 * geöffnete Detailseiten fließen in die Empfehlungen ein („Weil du nach „X“ gesucht hast“), die
 * Empfehlungen richten sich nach der Tageszeit. Gespeichert wird nur auf dem eigenen Server, wenn der
 * Admin es erlaubt und der Benutzer es nicht abgeschaltet hat (Einstellungen → Empfehlungen).
 */
object TvClientBehavior {
	private const val TIMEOUT_MS = 8_000
	private const val SEARCH_PAUSE_MS = 2_500L
	private const val CACHE_MS = 30 * 60 * 1000L

	data class Me(val enabled: Boolean, val retentionDays: Int, val maxEntries: Int)

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val json = Json { ignoreUnknownKeys = true }
	private var searchJob: Job? = null
	private var availableFor: String? = null
	private var available = false
	private var daypart: Triple<String, Long, List<String>>? = null

	private fun api() = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)

	/** Abweichung der Ortszeit von UTC in Minuten (z. B. 120 im Sommer). */
	fun utcOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

	private fun call(method: String, path: String, body: String? = null): Pair<Int, String> {
		val api = api()
		val base = api.baseUrl?.trimEnd('/') ?: error("keine Server-Adresse")
		val token = api.accessToken ?: error("nicht angemeldet")
		val connection = URL(base + path).openConnection() as HttpURLConnection
		return try {
			connection.requestMethod = method
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
			connection.setRequestProperty("Accept", "application/json")
			if (body != null) {
				connection.doOutput = true
				connection.setRequestProperty("Content-Type", "application/json")
				connection.outputStream.use { it.write(body.toByteArray()) }
			}
			val code = connection.responseCode
			code to (if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else "")
		} finally {
			connection.disconnect()
		}
	}

	/** Bietet der Server Verhaltenssignale an? (GET /LocalRecs/Web/Config → „BehaviorSignals“: true) */
	suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
		val base = api().baseUrl
		if (availableFor == base) return@withContext available
		available = runCatching {
			val config = json.parseToJsonElement(call("GET", "/LocalRecs/Web/Config").second) as? JsonObject
			(config?.get("BehaviorSignals") as? JsonPrimitive)?.booleanOrNull == true
		}.getOrDefault(false)
		availableFor = base
		available
	}

	/** Suchbegriff melden – erst nach 2,5 s Tipp-Pause, nur 2–80 Zeichen. */
	fun search(term: String) {
		searchJob?.cancel()
		val trimmed = term.trim()
		if (trimmed.length !in 2..80) return
		searchJob = scope.launch {
			delay(SEARCH_PAUSE_MS)
			if (!isAvailable()) return@launch
			runCatching { call("POST", "/LocalRecs/Behavior/Search", buildJsonObject { put("Term", trimmed) }.toString()) }
				.onFailure { Timber.d("Suche nicht gemeldet: %s", it.message) }
		}
	}

	/** Geöffnete Detailseite eines Films oder einer Serie melden. */
	@JvmStatic
	fun view(itemId: UUID) {
		scope.launch {
			if (!isAvailable()) return@launch
			runCatching { call("POST", "/LocalRecs/Behavior/View", buildJsonObject { put("ItemId", itemId.toString()) }.toString()) }
				.onFailure { Timber.d("Ansicht nicht gemeldet: %s", it.message) }
		}
	}

	/** Reihenfolge der Empfehlungszeilen für die aktuelle Tageszeit („movies“, „series“, „livetv“); null ohne Plugin. */
	suspend fun daypartOrder(): List<String>? = withContext(Dispatchers.IO) {
		val key = "${api().baseUrl}|${api().accessToken?.hashCode()}|${System.currentTimeMillis() / (60 * 60 * 1000L)}"
		daypart?.takeIf { it.first == key && System.currentTimeMillis() - it.second < CACHE_MS }?.let { return@withContext it.third }
		if (!isAvailable()) return@withContext null
		runCatching {
			val obj = json.parseToJsonElement(call("GET", "/LocalRecs/Behavior/Daypart?utcOffsetMinutes=${utcOffsetMinutes()}").second) as JsonObject
			(obj["Order"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content?.lowercase() }
		}.getOrNull()?.takeIf { it.isNotEmpty() }?.also { daypart = Triple(key, System.currentTimeMillis(), it) }
	}

	/** Eigener Stand (an/aus, Speicherdauer). */
	suspend fun me(): Me? = withContext(Dispatchers.IO) {
		if (!isAvailable()) return@withContext null
		runCatching {
			val obj = json.parseToJsonElement(call("GET", "/LocalRecs/Behavior/Me").second) as JsonObject
			Me(
				enabled = (obj["Enabled"] as? JsonPrimitive)?.booleanOrNull != false,
				retentionDays = (obj["RetentionDays"] as? JsonPrimitive)?.intOrNull ?: 365,
				maxEntries = (obj["MaxEntries"] as? JsonPrimitive)?.intOrNull ?: 1000,
			)
		}.getOrNull()
	}

	/** Für mich an- oder abschalten. */
	suspend fun setEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
		runCatching { call("POST", "/LocalRecs/Behavior/Me", buildJsonObject { put("Enabled", enabled) }.toString()).first in 200..299 }.getOrDefault(false)
	}

	/** Meine gespeicherten Suchen und Ansichten löschen. */
	suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
		runCatching { call("DELETE", "/LocalRecs/Behavior/Me").first in 200..299 }.getOrDefault(false)
	}

	/**
	 * Empfehlungszeilen in die Reihenfolge der Tageszeit bringen. Nur die Plätze der Empfehlungszeilen werden
	 * getauscht, alle anderen Zeilen bleiben, wo sie sind (testbar).
	 */
	fun <T> reorder(rows: List<T>, kindOf: (T) -> String?, order: List<String>?): List<T> {
		if (order.isNullOrEmpty()) return rows
		val slots = rows.indices.filter { kindOf(rows[it]) != null }
		val sorted = slots.map { rows[it] }.sortedBy { row -> order.indexOf(kindOf(row)).let { if (it < 0) Int.MAX_VALUE else it } }
		val result = rows.toMutableList()
		slots.forEachIndexed { i, slot -> result[slot] = sorted[i] }
		return result
	}
}
