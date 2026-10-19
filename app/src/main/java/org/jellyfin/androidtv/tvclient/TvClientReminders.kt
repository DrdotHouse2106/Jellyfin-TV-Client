package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.TextUnderButton
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

/**
 * Jellyfin-TV-Client: Live-TV-Sendungen „merken“ (Plugin Local Recommendations ab 12.20). Gemerkte
 * Sendungen landen in einem Kalender, den man auf dem Handy abonniert (QR-Code in den Einstellungen).
 */
object TvClientReminders {
	private const val TIMEOUT_MS = 8_000

	data class Calendar(val url: String, val webcalUrl: String)

	/** Gemerkte Sendungen (Id ohne Bindestriche) → aus einer gemerkten Serie? */
	private val _remembered = MutableStateFlow<Map<String, Boolean>>(emptyMap())
	val remembered = _remembered.asStateFlow()

	private var availableFor: String? = null
	private var available = false

	private val json = Json { ignoreUnknownKeys = true }

	private fun api() = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)

	fun key(id: UUID?): String = id?.toString()?.replace("-", "")?.lowercase().orEmpty()

	private class Response(val code: Int, val body: String)

	private fun call(method: String, path: String, api: ApiClient = api()): Response {
		val base = api.baseUrl?.trimEnd('/') ?: error("keine Server-Adresse")
		val token = api.accessToken ?: error("nicht angemeldet")
		val connection = URL(base + path).openConnection() as HttpURLConnection
		return try {
			connection.requestMethod = method
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
			connection.setRequestProperty("Accept", "application/json")
			if (method == "POST") {
				connection.doOutput = true
				connection.setFixedLengthStreamingMode(0)
			}
			val code = connection.responseCode
			val body = if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else ""
			Response(code, body)
		} finally {
			connection.disconnect()
		}
	}

	/** Bietet der Server Erinnerungen an? (GET /LocalRecs/Web/Config → „Reminders“: true) */
	suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
		val base = api().baseUrl
		if (availableFor == base) return@withContext available
		available = runCatching {
			val config = json.parseToJsonElement(call("GET", "/LocalRecs/Web/Config").body) as? JsonObject
			(config?.get("Reminders") as? JsonPrimitive)?.booleanOrNull == true
		}.getOrDefault(false)
		availableFor = base
		available
	}

	/** Liste der gemerkten Sendungen neu laden. */
	suspend fun refresh() = withContext(Dispatchers.IO) {
		runCatching {
			_remembered.value = parseReminders(call("GET", "/LocalRecs/Reminders").body)
		}.onFailure { Timber.w(it, "Erinnerungen nicht verfügbar") }
	}

	/** Antwort von GET /LocalRecs/Reminders: Id (ohne Bindestriche) → aus einer gemerkten Serie. */
	fun parseReminders(text: String): Map<String, Boolean> =
		(json.parseToJsonElement(text) as? JsonArray).orEmpty()
			.mapNotNull { it as? JsonObject }
			.mapNotNull { entry ->
				val id = (entry["ProgramId"] as? JsonPrimitive)?.content?.replace("-", "")?.lowercase() ?: return@mapNotNull null
				id to ((entry["IsSeries"] as? JsonPrimitive)?.booleanOrNull == true)
			}
			.toMap()

	/** Sendung merken bzw. vergessen. Gibt den neuen Zustand zurück, null bei Fehler. */
	suspend fun toggle(programId: UUID): Int? = withContext(Dispatchers.IO) {
		val id = key(programId)
		val remembered = _remembered.value.containsKey(id)
		runCatching {
			call(if (remembered) "DELETE" else "POST", "/LocalRecs/Reminders/$id").code.also { refresh() }
		}.getOrNull()
	}

	/** Serie dieser Sendung merken bzw. vergessen. */
	suspend fun toggleSeries(programId: UUID): Int? = withContext(Dispatchers.IO) {
		val id = key(programId)
		val series = _remembered.value[id] == true
		runCatching {
			call(if (series) "DELETE" else "POST", "/LocalRecs/Reminders/Series/$id").code.also { refresh() }
		}.getOrNull()
	}

	/** Abo-Links für den Kalender (Token entsteht beim ersten Aufruf). */
	suspend fun calendar(renew: Boolean = false): Calendar? = withContext(Dispatchers.IO) {
		runCatching {
			val response = call(if (renew) "POST" else "GET", if (renew) "/LocalRecs/Calendar/Renew" else "/LocalRecs/Calendar")
			val obj = json.parseToJsonElement(response.body) as JsonObject
			Calendar(
				url = (obj["Url"] as JsonPrimitive).content,
				webcalUrl = (obj["WebcalUrl"] as? JsonPrimitive)?.content ?: (obj["Url"] as JsonPrimitive).content.replaceFirst(Regex("^https?://"), "webcal://"),
			)
		}.onFailure { Timber.w(it, "Kalender nicht verfügbar") }.getOrNull()
	}

	/** QR-Code als SVG vom Server (target: „webcal“ fürs iPhone, „https“ für andere). */
	fun qrUrl(target: String): String? {
		val api = api()
		val base = api.baseUrl?.trimEnd('/') ?: return null
		return "$base/LocalRecs/Calendar/Qr?target=$target&ApiKey=${api.accessToken}"
	}

	private fun message(context: Context, code: Int?) = when (code) {
		204, 200 -> null
		409 -> R.string.tvclient_reminder_over
		403 -> R.string.tvclient_reminder_forbidden
		else -> R.string.tvclient_reminder_failed
	}?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }

	/**
	 * Knöpfe „Merken“ und (bei Serien) „Serie merken“ an eine Sendung binden. Sie bleiben verborgen,
	 * bis klar ist, dass der Server Erinnerungen anbietet, und zeigen dann „Gemerkt“ bzw. „Serie gemerkt“.
	 */
	@JvmStatic
	fun bind(lifecycle: Lifecycle, program: BaseItemDto, button: View, seriesButton: View?) {
		val id = program.id
		button.visibility = View.GONE
		seriesButton?.visibility = View.GONE
		if (program.endDate?.isBefore(LocalDateTime.now()) == true) return

		fun update() {
			val state = _remembered.value[key(id)]
			label(button, if (state != null) R.string.tvclient_reminder_saved else R.string.tvclient_reminder_save, state != null)
			seriesButton?.let { label(it, if (state == true) R.string.tvclient_reminder_series_saved else R.string.tvclient_reminder_series_save, state == true) }
		}

		lifecycle.coroutineScope.launch {
			if (!isAvailable()) return@launch
			refresh()
			button.visibility = View.VISIBLE
			if (program.isSeries == true) seriesButton?.visibility = View.VISIBLE
			update()
			button.setOnClickListener {
				lifecycle.coroutineScope.launch {
					message(button.context, toggle(id))
					update()
				}
			}
			seriesButton?.setOnClickListener {
				lifecycle.coroutineScope.launch {
					message(it.context, toggleSeries(id))
					update()
				}
			}
		}
	}

	private fun label(view: View, text: Int, active: Boolean) {
		val label = view.context.getString(text)
		when (view) {
			is TextUnderButton -> {
				view.setLabel(label)
				view.setIcon(if (active) R.drawable.ic_check else R.drawable.ic_add)
				view.isActivated = active
			}
			is Button -> view.text = label
		}
	}
}
