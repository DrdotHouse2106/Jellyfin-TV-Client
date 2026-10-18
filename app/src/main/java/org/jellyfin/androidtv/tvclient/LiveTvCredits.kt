package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Jellyfin-TV-Client: Besetzung und Regie für Live-TV-Sendungen. Jellyfin selbst kennt für
 * Sendungen keine Personen – das Plugin „Local Recommendations“ (ab 12.7.1) liefert sie aus dem EPG.
 * Ohne Plugin, ohne EPG-Daten oder ohne Treffer bleibt alles still weg.
 */
object LiveTvCredits {
	private const val TIMEOUT_MS = 8_000
	// Im Popup höchstens so viele Darsteller, damit die Knöpfe darunter sichtbar bleiben
	private const val MAX_POPUP_ACTORS = 4

	/** EPG-Texte enthalten teils wörtlich „\n“ – in echte Zeilenumbrüche umwandeln. */
	@JvmStatic
	fun cleanText(text: String?): String = text.orEmpty()
		.replace("\\n", "\n")
		.replace(Regex("\n{3,}"), "\n\n")
		.trim()

	private val json = Json { ignoreUnknownKeys = true }

	data class Actor(val name: String, val role: String?)
	data class Credits(val actors: List<Actor>, val directors: List<String>, val writers: List<String>, val year: Int?, val episode: String?) {
		val isEmpty get() = actors.isEmpty() && directors.isEmpty()
	}

	private fun api() = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)

	/** Protokoll-Tag für logcat (`adb logcat -s Quallenkino`), auch in Release-Builds. */
	private const val LOG_TAG = "Quallenkino"

	private fun call(api: ApiClient, path: String): String? {
		val base = api.baseUrl?.trimEnd('/') ?: return null.also { Log.i(LOG_TAG, "Besetzung: keine Server-Adresse") }
		val token = api.accessToken ?: return null.also { Log.i(LOG_TAG, "Besetzung: nicht angemeldet") }
		val connection = URL(base + path).openConnection() as HttpURLConnection
		return try {
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
			connection.setRequestProperty("Accept", "application/json")
			val code = connection.responseCode
			Log.i(LOG_TAG, "Besetzung: GET $path → HTTP $code")
			if (code !in 200..299) null
			else connection.inputStream.bufferedReader().use { it.readText() }
		} finally {
			connection.disconnect()
		}
	}

	private fun JsonObject.string(key: String): String? =
		(this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

	private fun JsonObject.strings(key: String): List<String> =
		(this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.takeIf { s -> s.isNotBlank() } }

	/** Antwort des Plugins auswerten; ohne Treffer null. */
	fun parse(text: String): Credits? {
		val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
		if ((obj["Matched"] as? JsonPrimitive)?.booleanOrNull != true) return null
		val actors = (obj["Actors"] as? JsonArray).orEmpty().mapNotNull { element ->
			val actor = element as? JsonObject ?: return@mapNotNull null
			Actor(actor.string("Name") ?: return@mapNotNull null, actor.string("Role"))
		}
		return Credits(
			actors = actors,
			directors = obj.strings("Directors"),
			writers = obj.strings("Writers"),
			year = (obj["Year"] as? JsonPrimitive)?.intOrNull,
			episode = obj.string("Episode"),
		).takeIf { !it.isEmpty }
	}

	suspend fun load(programId: UUID, api: ApiClient = api()): Credits? = withContext(Dispatchers.IO) {
		runCatching {
			call(api, "/LocalRecs/LiveTv/Program/${programId.toString().replace("-", "")}/Credits")?.let(::parse)
				.also { Log.i(LOG_TAG, "Besetzung: ${it?.actors?.size ?: 0} Darsteller, ${it?.directors?.size ?: 0} Regie") }
		}.onFailure { Log.w(LOG_TAG, "Besetzung für Sendung nicht verfügbar", it) }.getOrNull()
	}

	/** „Regie: …“ und „Mit: A (Rolle), B …“ als Text, z. B. für das Popup im Programmführer. */
	fun summaryLines(context: Context, credits: Credits): String = buildList {
		if (credits.directors.isNotEmpty()) add(context.getString(R.string.tvclient_livetv_director, credits.directors.joinToString(", ")))
		if (credits.actors.isNotEmpty()) add(context.getString(R.string.tvclient_livetv_with, credits.actors.take(MAX_POPUP_ACTORS).joinToString(", ") { actor ->
			if (actor.role != null) "${actor.name} (${actor.role})" else actor.name
		}))
	}.joinToString("\n")

	/** Popup im Programmführer: Besetzung unter die Beschreibung schreiben. */
	@JvmStatic
	fun appendToSummary(lifecycle: Lifecycle, summary: TextView, program: BaseItemDto) {
		val programId = program.id
		summary.tag = programId
		Log.i(LOG_TAG, "Besetzung: Programmführer-Popup für $programId (${lifecycle.currentState})")
		lifecycle.coroutineScope.launch {
			val credits = load(programId) ?: return@launch
			if (summary.tag != programId) {
				Log.i(LOG_TAG, "Besetzung: Popup zeigt schon eine andere Sendung")
				return@launch
			}
			val overview = cleanText(program.overview)
			val lines = summaryLines(summary.context, credits)
			// Vor die Beschreibung, sonst schneidet maxLines die Besetzung bei langen Texten ab
			summary.text = if (overview.isBlank()) lines else "$lines\n\n$overview"
			Log.i(LOG_TAG, "Besetzung: im Popup angezeigt")
			summary.gravity = Gravity.START or Gravity.CENTER_VERTICAL
		}
	}

	/** Detailseite einer Sendung: Reihe „Besetzung“ (mit Regie im Titel) anhängen. */
	@JvmStatic
	fun addRow(lifecycle: Lifecycle, context: Context, rows: org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter<Row>, item: BaseItemDto) {
		if (item.type != BaseItemKind.PROGRAM) return
		lifecycle.coroutineScope.launch {
			val credits = load(item.id) ?: return@launch
			if (credits.actors.isEmpty()) return@launch
			val header = buildString {
				append(context.getString(R.string.lbl_cast_crew))
				if (credits.directors.isNotEmpty()) append(" · ").append(context.getString(R.string.tvclient_livetv_director, credits.directors.joinToString(", ")))
			}
			val actors = ArrayObjectAdapter(ActorPresenter())
			credits.actors.forEach(actors::add)
			rows.add(ListRow(HeaderItem(header), actors))
		}
	}

	/** OK auf einen Darsteller: bei Jellyseerr nach genau diesem Namen suchen und die Personenseite öffnen. */
	@JvmStatic
	fun open(lifecycle: Lifecycle, actor: Any?): Boolean {
		if (actor !is Actor) return false
		lifecycle.coroutineScope.launch {
			val id = runCatching {
				if (Jellyseerr.detect(api()) == null) null else Jellyseerr.findPerson(actor.name)
			}.getOrNull() ?: return@launch
			KoinJavaComponent.get<NavigationRepository>(NavigationRepository::class.java).navigate(Destinations.jellyseerrPerson(id))
		}
		return true
	}

	/** Darsteller ohne Foto (das EPG hat keine): Initialen im Kreis, Name und Rolle darunter. */
	class ActorPresenter : Presenter() {
		override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
			val context = parent.context
			fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
			val circle = TextView(context).apply {
				gravity = Gravity.CENTER
				setTextColor(Color.WHITE)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
				typeface = Typeface.DEFAULT_BOLD
				background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#333333")) }
			}
			fun label(size: Float, color: Int) = TextView(context).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
				setTextColor(color)
				maxLines = 2
				ellipsize = TextUtils.TruncateAt.END
			}
			val name = label(13f, Color.WHITE)
			val role = label(11f, Color.parseColor("#B3B3B3"))
			val root = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				gravity = Gravity.CENTER_HORIZONTAL
				layoutParams = ViewGroup.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT)
				setPadding(dp(4), dp(6), dp(4), dp(6))
				isFocusable = true
				isFocusableInTouchMode = true
				addView(circle, LinearLayout.LayoutParams(dp(90), dp(90)))
				addView(name, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
				addView(role, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
				setOnFocusChangeListener { view, focused ->
					val scale = if (focused) 1.1f else 1f
					view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
					(circle.background as GradientDrawable).setStroke(if (focused) dp(3) else 0, Color.WHITE)
				}
			}
			root.tag = Triple(circle, name, role)
			return ViewHolder(root)
		}

		override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
			val actor = item as? Actor ?: return
			@Suppress("UNCHECKED_CAST")
			val views = viewHolder.view.tag as Triple<TextView, TextView, TextView>
			val (circle, name, role) = views
			circle.text = initials(actor.name)
			name.text = actor.name
			role.text = actor.role.orEmpty()
		}

		override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
	}

	fun initials(name: String): String = name.split(' ', '-')
		.filter { it.isNotBlank() }
		.let { parts -> listOfNotNull(parts.firstOrNull(), parts.drop(1).lastOrNull()) }
		.joinToString("") { it.first().uppercase() }
}
