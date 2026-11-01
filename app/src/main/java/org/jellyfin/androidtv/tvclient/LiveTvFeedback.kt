package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.net.URLEncoder

/**
 * Jellyfin-TV-Client: „Heute im TV für dich“ – Rückmeldung „Seltener vorschlagen“, „Weniger <Genre>“ und
 * „Nie mehr vorschlagen“ (Plugin Local Recommendations ab 12.58, POST/DELETE /LocalRecs/LiveTv/Feedback).
 * Gilt auf dem Server für die ganze Sendung bzw. Serie und damit auch im Web und auf anderen Geräten.
 * Danach verschwindet die Karte sofort; ein kurzer Hinweis bietet „Rückgängig“.
 */
object LiveTvFeedback {
	enum class Kind(val wire: String) { LESS("weniger"), LESS_GENRE("genre_weniger"), NEVER("nie") }

	/** Antwort des Servers: Key der Sendung bzw. das Genre – damit lässt sich die Markierung zurücknehmen. */
	data class Mark(val kind: Kind, val key: String?, val genre: String?)

	/** Diese Genres sagen nichts über den Geschmack (wie auf dem Server übersprungen). */
	private val GENERIC_GENRES = setOf("series", "movie", "sports", "news", "kids", "serie", "film", "sport", "nachrichten", "kinder")

	private val json = Json { ignoreUnknownKeys = true }
	private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
	private const val UNDO_VISIBLE_MS = 8_000L

	/** Auf der Sendungsseite markiert: die Startseite entfernt diese Karten beim Zurückkehren einmalig. */
	private val pendingTitles = mutableSetOf<String>()

	/** Gibt die gemerkten Sendungsnamen (klein geschrieben) zurück und leert die Liste. */
	fun consumePendingTitles(): Set<String> = pendingTitles.toSet().also { pendingTitles.clear() }

	/** Erstes aussagekräftiges Genre der Sendung (für „Weniger Krimi“), sonst null. */
	fun genreOf(program: BaseItemDto): String? =
		program.genres.orEmpty().map { it.trim() }.firstOrNull { it.isNotEmpty() && it.lowercase() !in GENERIC_GENRES }

	/** Sendungsname für Abgleich und Anzeige (bei Folgen der Serienname). */
	fun titleOf(program: BaseItemDto): String? = (program.seriesName ?: program.name)?.trim()?.takeIf { it.isNotEmpty() }

	/** Body für POST /LocalRecs/LiveTv/Feedback (testbar). */
	fun body(program: BaseItemDto, kind: Kind): String = buildJsonObject {
		put("ProgramId", program.id.toString())
		program.seriesId?.let { put("SeriesId", it.toString()) }
		titleOf(program)?.let { put("Title", it) }
		if (kind == Kind.LESS_GENRE) genreOf(program)?.let { put("Genre", it) }
		put("Art", kind.wire)
	}.toString()

	/** Adresse zum Zurücknehmen (DELETE). */
	fun undoPath(mark: Mark, program: BaseItemDto): String {
		fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
		return when {
			mark.kind == Kind.LESS_GENRE -> "/LocalRecs/LiveTv/Feedback?genre=${enc(mark.genre ?: genreOf(program).orEmpty())}"
			mark.key != null -> "/LocalRecs/LiveTv/Feedback?key=${enc(mark.key)}"
			else -> "/LocalRecs/LiveTv/Feedback?programId=${enc(program.id.toString())}"
		}
	}

	fun parseMark(kind: Kind, text: String): Mark {
		val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
		fun field(name: String) = (obj?.get(name) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
		return Mark(kind, field("Key"), field("Genre"))
	}

	private suspend fun send(program: BaseItemDto, kind: Kind): Mark? = withContext(Dispatchers.IO) {
		runCatching {
			val (code, text) = TvClientBehavior.call("POST", "/LocalRecs/LiveTv/Feedback", body(program, kind))
			if (code in 200..299) parseMark(kind, text) else null.also { Timber.w("LiveTv-Feedback: HTTP %d", code) }
		}.onFailure { Timber.w(it, "LiveTv-Feedback fehlgeschlagen") }.getOrNull()
	}

	private suspend fun undo(program: BaseItemDto, mark: Mark): Boolean = withContext(Dispatchers.IO) {
		runCatching { TvClientBehavior.call("DELETE", undoPath(mark, program)).first.let { it in 200..299 || it == 404 } }
			.onFailure { Timber.w(it, "LiveTv-Feedback: Rückgängig fehlgeschlagen") }.getOrDefault(false)
	}

	/**
	 * Menü für eine Sendung. [onApplied] entfernt die Karte(n) sofort, [onUndone] holt die Reihe nach
	 * „Rückgängig“ neu.
	 */
	fun showMenu(context: Context, program: BaseItemDto, onApplied: (Kind) -> Unit = {}, onUndone: () -> Unit = {}) {
		val genre = genreOf(program)
		val buttons = listOfNotNull(
			TvInfoDialog.Button(context.getString(R.string.tvclient_livetv_less)) { apply(context, program, Kind.LESS, onApplied, onUndone) },
			genre?.let { TvInfoDialog.Button(context.getString(R.string.tvclient_livetv_less_genre, it)) { apply(context, program, Kind.LESS_GENRE, onApplied, onUndone) } },
			TvInfoDialog.Button(context.getString(R.string.tvclient_livetv_never)) { apply(context, program, Kind.NEVER, onApplied, onUndone) },
		)
		TvInfoDialog.show(context, titleOf(program) ?: program.name.orEmpty(), emptyList(), buttons, verticalButtons = true)
	}

	private fun apply(context: Context, program: BaseItemDto, kind: Kind, onApplied: (Kind) -> Unit, onUndone: () -> Unit) {
		scope.launch {
			val mark = send(program, kind)
			if (mark == null) {
				Toast.makeText(context, R.string.tvclient_livetv_feedback_failed, Toast.LENGTH_SHORT).show()
				return@launch
			}
			val title = titleOf(program)?.lowercase()
			if (kind != Kind.LESS_GENRE && title != null) pendingTitles.add(title)
			onApplied(kind)
			val message = when (kind) {
				Kind.LESS -> context.getString(R.string.tvclient_livetv_less_done)
				Kind.NEVER -> context.getString(R.string.tvclient_livetv_never_done)
				Kind.LESS_GENRE -> context.getString(R.string.tvclient_livetv_genre_done, mark.genre ?: genreOf(program).orEmpty())
			}
			val dialog = TvInfoDialog.show(
				context, message, listOf(TvInfoDialog.Line(titleOf(program).orEmpty(), TvInfoDialog.Style.MUTED)),
				listOf(
					TvInfoDialog.Button(context.getString(R.string.tvclient_livetv_ok)),
					TvInfoDialog.Button(context.getString(R.string.tvclient_livetv_undo)) {
						scope.launch {
							if (undo(program, mark)) {
								title?.let { pendingTitles.remove(it) }
								onUndone()
							}
						}
					},
				),
			)
			// Kurzer Hinweis: schließt sich von selbst
			Handler(Looper.getMainLooper()).postDelayed({ if (dialog.isShowing) runCatching { dialog.dismiss() } }, UNDO_VISIBLE_MS)
		}
	}
}
