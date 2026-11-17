package org.jellyfin.androidtv.tvclient

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.redactSecrets
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import timber.log.Timber
import java.util.ArrayDeque

/**
 * Jellyfin-TV-Client: „Ups, gab es hier ein technisches Problem?“
 * Wird ein Film oder eine Folge zweimal nach wenigen Minuten beendet (oder bricht mit Player-Fehler ab), fragt Quallenkino
 * beim nächsten Start dieses Titels, ob es einen Bericht an den Server schicken soll (Plugin Local Recommendations ab
 * 12.63: Reiter „Problemberichte“). Im Player geht es jederzeit über „Problem melden“. Der Bericht enthält Gerät,
 * Wiedergabeart, Spuren, Ton-Ausgang, Fehlermeldung und die letzten Log-Zeilen – Zugangsdaten geschwärzt.
 */
object ProblemReports {
	/** Kürzer geschaut gilt als Abbruch. */
	private const val ABORT_MS = 2 * 60 * 1000L

	/** Nur Titel ab dieser Laufzeit (keine Trailer, Extras). */
	private const val MIN_RUNTIME_MS = 10 * 60 * 1000L

	/** Ab so vielen Abbrüchen fragen. */
	const val ASK_AFTER = 2

	private const val LOG_LINES = 100
	private const val PREFS = "tvclient_problems"

	private val log = ArrayDeque<String>()

	@Volatile private var lastError: String? = null

	/** Letzte Log-Zeilen sammeln (im Speicher, nur für einen Bericht). */
	val logTree = object : Timber.Tree() {
		override fun isLoggable(tag: String?, priority: Int) = priority >= android.util.Log.INFO

		override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
			val level = when (priority) {
				android.util.Log.WARN -> "W"
				android.util.Log.ERROR -> "E"
				else -> "I"
			}
			val line = "${java.time.LocalTime.now().withNano(0)} $level ${tag.orEmpty()}: ${redactSecrets(message)}" +
				(t?.let { " – ${it.javaClass.simpleName}: ${it.message}" } ?: "")
			synchronized(log) {
				log.addLast(line.take(800))
				while (log.size > LOG_LINES) log.removeFirst()
			}
		}
	}

	private fun logLines(): List<String> = synchronized(log) { log.toList() }

	/** Fehler des Players merken (für den Bericht). */
	@JvmStatic
	fun onPlayerError(error: Throwable) {
		lastError = listOfNotNull(
			(error as? androidx.media3.common.PlaybackException)?.errorCodeName,
			error.message,
			error.cause?.let { "${it.javaClass.simpleName}: ${it.message}" },
		).joinToString(" – ")
	}

	/** Testbar: zählt das Ende der Wiedergabe als Abbruch? */
	fun isAbort(positionMs: Long, runtimeMs: Long?, error: Boolean): Boolean =
		runtimeMs != null && runtimeMs >= MIN_RUNTIME_MS && (error || positionMs < ABORT_MS)

	private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

	/**
	 * Ende einer Wiedergabe (Player geschlossen oder nach Fehlern abgebrochen). Bei einem Abbruch werden Zähler und
	 * Zustand (Wiedergabeart, Spuren …) für einen späteren Bericht gemerkt, sonst zurückgesetzt.
	 */
	@JvmStatic
	fun onPlaybackEnded(context: Context, controller: PlaybackController, positionMs: Long, error: Boolean) {
		val item = controller.currentlyPlayingItem ?: return
		if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.EPISODE) return
		val key = item.id.toString()
		val prefs = prefs(context)
		if (!isAbort(positionMs, item.runTimeTicks?.div(10_000), error)) {
			if (prefs.contains("count_$key")) prefs.edit().remove("count_$key").remove("state_$key").apply()
			return
		}
		val count = prefs.getInt("count_$key", 0) + 1
		Timber.i("Problemerkennung: Titel nach %d s beendet (%d. Mal)%s", positionMs / 1000, count, if (error) ", Player-Fehler" else "")
		prefs.edit().putInt("count_$key", count).putString("state_$key", state(controller).joinToString("\n")).apply()
	}

	/**
	 * Start eines Titels: Nach [ASK_AFTER] Abbrüchen fragen, ob ein Bericht geschickt werden soll.
	 * „Nein, passt“ fragt für diesen Titel nicht mehr.
	 */
	@JvmStatic
	fun onPlaybackStarted(context: Context, item: BaseItemDto?) {
		item ?: return
		val key = item.id.toString()
		val prefs = prefs(context)
		val count = prefs.getInt("count_$key", 0)
		if (count < ASK_AFTER || prefs.getBoolean("dismissed_$key", false)) return
		val state = prefs.getString("state_$key", null)?.split("\n").orEmpty()
		AlertDialog.Builder(context)
			.setTitle(R.string.tvclient_problem_title)
			.setMessage(context.getString(R.string.tvclient_problem_message, count))
			.setPositiveButton(R.string.tvclient_problem_report) { _, _ ->
				send(context, item, context.getString(R.string.tvclient_problem_summary_aborts, count), state)
				prefs.edit().remove("count_$key").remove("state_$key").apply()
			}
			.setNegativeButton(R.string.tvclient_problem_no) { _, _ ->
				prefs.edit().putBoolean("dismissed_$key", true).remove("count_$key").remove("state_$key").apply()
			}
			.show()
	}

	/** „Problem melden“ im Player. */
	@JvmStatic
	fun reportFromPlayer(context: Context, controller: PlaybackController) {
		val item = controller.currentlyPlayingItem ?: return
		val state = state(controller)
		AlertDialog.Builder(context)
			.setTitle(R.string.tvclient_problem_report_title)
			.setMessage(R.string.tvclient_problem_report_message)
			.setPositiveButton(R.string.tvclient_problem_report) { _, _ ->
				send(context, item, context.getString(R.string.tvclient_problem_summary_manual), state)
			}
			.setNegativeButton(R.string.lbl_cancel, null)
			.show()
	}

	/** Zustand der Wiedergabe als Zeilen „Feld=Wert“ (für den Bericht). */
	private fun state(controller: PlaybackController): List<String> = runCatching {
		val info = controller.currentStreamInfo
		val source = controller.currentMediaSource
		val streams = source?.mediaStreams.orEmpty()
		val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
		val audio = streams.firstOrNull { it.type == MediaStreamType.AUDIO && it.index == controller.audioStreamIndex }
		val subtitle = streams.firstOrNull { it.type == MediaStreamType.SUBTITLE && it.index == controller.subtitleStreamIndex }
		val method = when (info?.playMethod) {
			PlayMethod.TRANSCODE -> "Umwandlung"
			PlayMethod.DIRECT_STREAM -> "Direct Stream"
			PlayMethod.DIRECT_PLAY -> "Direktwiedergabe"
			null -> "?"
		}
		// Ziel der Umwandlung und Gründe aus der Adresse (ohne Zugangsdaten)
		val transcode = source?.transcodingUrl?.let { url ->
			val uri = Uri.parse("http://x$url")
			listOf("VideoCodec", "AudioCodec", "AudioChannels", "TranscodeReasons")
				.mapNotNull { name -> uri.getQueryParameter(name)?.let { "$name=$it" } }
				.joinToString(", ")
		}
		listOf(
			"playback=" + listOfNotNull(
				method,
				info?.container,
				video?.let { "Bild ${it.codec} ${it.profile.orEmpty()} ${it.width}x${it.height} ${it.videoRangeType}".trim() },
				audio?.let { "Ton ${it.codec} ${it.channels} Kanäle" },
				transcode?.takeIf { it.isNotEmpty() }?.let { "Server: $it" },
			).joinToString(" · "),
			"tracks=" + listOf(
				"Ton: " + (audio?.displayTitle ?: "–"),
				"Untertitel: " + (subtitle?.displayTitle ?: "aus"),
			).joinToString(" · "),
			"output=" + TvClientSound.lastReport.orEmpty(),
			"error=" + lastError.orEmpty(),
		)
	}.getOrDefault(emptyList())

	private fun field(state: List<String>, name: String) = state.firstOrNull { it.startsWith("$name=") }?.substringAfter('=').orEmpty()

	private fun send(context: Context, item: BaseItemDto, summary: String, state: List<String>) {
		val body = buildJsonObject {
			put("ItemId", item.id.toString())
			put("Device", "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, Quallenkino ${BuildConfig.VERSION_NAME}")
			put("Summary", summary)
			put("Playback", field(state, "playback"))
			put("Tracks", field(state, "tracks"))
			put("Output", field(state, "output"))
			put("Error", field(state, "error"))
			put("Log", JsonArray(logLines().map { JsonPrimitive(it) }))
		}.toString()
		val appContext = context.applicationContext
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val code = withContext(Dispatchers.IO) {
				runCatching { TvClientBehavior.call("POST", "/LocalRecs/ProblemReports", body).first }
					.onFailure { Timber.w(it, "Problembericht nicht gesendet") }.getOrDefault(0)
			}
			val text = when (code) {
				in 200..299 -> R.string.tvclient_problem_sent
				404 -> R.string.tvclient_problem_no_plugin
				else -> R.string.tvclient_problem_failed
			}
			Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()
		}
	}
}
