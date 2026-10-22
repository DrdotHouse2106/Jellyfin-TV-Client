package org.jellyfin.androidtv.tvclient

import android.content.Context
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.stringPreference
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.Locale

/**
 * Jellyfin-TV-Client: Untertitel wie bei Netflix.
 * - Läuft der Ton in der eigenen Sprache (z. B. deutsche Synchro), werden keine Untertitel
 *   eingeblendet – außer erzwungene (Forced) Untertitel.
 * - Schaltet man Untertitel bei einer Serie aus oder um, gilt das auch für die nächsten Folgen.
 */
object TvClientSubtitles {
	val smartPreference = booleanPreference("pref_tvclient_smart_subtitles", true)

	/**
	 * Untertitel beim Start (wie in der webOS-App): „smart“ (intelligent), „always“ (immer in der
	 * eigenen Sprache), „off“ (nur erzwungene) oder „server“ (Vorschlag des Servers). Leer = aus der
	 * früheren Einstellung „Intelligente Untertitel“ ableiten.
	 */
	val modePreference = stringPreference("pref_tvclient_subtitle_mode", "")

	const val SMART = "smart"
	const val ALWAYS = "always"
	const val SERVER = "server"
	const val NONE = "off"

	/** Aktueller Modus (mit Übernahme der alten Ein/Aus-Einstellung). */
	fun mode(): String = runCatching {
		val prefs = KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
		prefs[modePreference].ifEmpty { if (prefs[smartPreference]) SMART else SERVER }
	}.getOrDefault(SMART)

	private const val OFF = "off"
	private const val PREFS = "tvclient_subtitles"


	private fun prefs() = KoinJavaComponent.get<Context>(Context::class.java)
		.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

	/** ISO-639-2/B-Codes (z. B. „ger“), die Java nicht kennt, auf ISO-639-2/T abbilden. */
	private val bibliographic = mapOf(
		"ger" to "deu", "fre" to "fra", "chi" to "zho", "dut" to "nld", "cze" to "ces", "gre" to "ell",
		"per" to "fas", "rum" to "ron", "slo" to "slk", "alb" to "sqi", "arm" to "hye", "baq" to "eus",
		"bur" to "mya", "geo" to "kat", "ice" to "isl", "mac" to "mkd", "mao" to "mri", "may" to "msa",
		"tib" to "bod", "wel" to "cym",
	)

	private val iso3ToIso2 by lazy {
		Locale.getISOLanguages().associateBy { runCatching { Locale.forLanguageTag(it).isO3Language }.getOrDefault(it) }
	}

	/** Sprachcode vereinheitlichen: „de“, „deu“, „ger“, „German“ → „de“. */
	fun normalize(language: String?): String? {
		val code = language?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
		if (code.isNullOrEmpty() || code == "und" || code == "unknown") return null
		return when (code.length) {
			2 -> code
			3 -> iso3ToIso2[bibliographic[code] ?: code] ?: code
			else -> Locale.getAvailableLocales()
				.firstOrNull { it.getDisplayLanguage(Locale.ENGLISH).equals(code, true) || it.displayLanguage.equals(code, true) }
				?.language ?: code
		}
	}

	/** Sprachen, die als „eigene Sprache“ gelten: Gerät, App und Jellyfin-Benutzereinstellung. */
	fun ownLanguages(): Set<String> = buildSet {
		add(Locale.getDefault().language)
		runCatching {
			val context = KoinJavaComponent.get<Context>(Context::class.java)
			add(context.resources.configuration.locales[0].language)
		}
		runCatching {
			val user = KoinJavaComponent.get<UserRepository>(UserRepository::class.java).currentUser.value
			add(user?.configuration?.audioLanguagePreference.orEmpty())
		}
	}.mapNotNull { normalize(it) }.toSet()

	/**
	 * Untertitel für den Start eines Titels. Gibt -1 (aus), einen Stream-Index oder den Vorschlag
	 * des Servers zurück.
	 */
	@JvmStatic
	fun chooseSubtitleIndex(item: BaseItemDto?, source: MediaSourceInfo?, serverDefault: Int?, audioIndex: Int?): Int? {
		val mode = mode()
		if (item == null || source == null || mode == SERVER) return serverDefault
		val streams = source.mediaStreams.orEmpty()
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val own = ownLanguages()

		// Feste Modi: aus (nur erzwungene) bzw. immer in der eigenen Sprache
		if (mode == NONE) return forcedIndex(subtitles, setOfNotNull(audioLanguage(streams, audioIndex)) + own) ?: -1
		if (mode == ALWAYS) return alwaysOwn(subtitles, own) ?: serverDefault

		// Für diese Serie gemerkte Wahl
		val remembered = item.seriesId?.let { prefs().getString(it.toString(), null) }
		if (remembered == OFF) return forcedIndex(subtitles, setOfNotNull(audioLanguage(streams, audioIndex)) + own) ?: -1
		// Gemerkte Untertitel in der Sprache des Tons ignorieren (deutscher Ton + deutsche Untertitel)
		if (remembered != null && normalize(remembered) != audioLanguage(streams, audioIndex)) {
			subtitles.firstOrNull { normalize(it.language) == normalize(remembered) && !isForced(it) }
				?.let { return it.index }
		}

		val result = decide(streams, serverDefault, audioIndex, own)
		Timber.i("Untertitel: Ton=%s eigene=%s Server=%s -> %s", audioLanguage(streams, audioIndex), own, serverDefault, result)
		return result
	}

	private fun audioLanguage(streams: List<MediaStream>, audioIndex: Int?): String? {
		val audios = streams.filter { it.type == MediaStreamType.AUDIO }
		val audio = audios.firstOrNull { it.index == audioIndex }
			?: audios.firstOrNull { it.isDefault == true }
			?: audios.firstOrNull()
		return normalize(audio?.language)
	}

	/**
	 * Reine Entscheidung (testbar):
	 * - Ton in der eigenen Sprache: keine Untertitel, nur erzwungene in der Sprache des Tons.
	 * - Fremder Ton (z. B. Originalton): volle Untertitel in der eigenen Sprache – auch wenn der Server
	 *   erzwungene oder keine vorschlägt (sein Vorschlag gilt meist für den Standardton).
	 * - Gibt es keine eigenen, Untertitel in der Sprache des Tons weglassen, sonst den Vorschlag des Servers.
	 */
	fun decide(streams: List<MediaStream>, serverDefault: Int?, audioIndex: Int?, ownLanguages: Set<String>): Int? {
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val audioLanguage = audioLanguage(streams, audioIndex) ?: return serverDefault
		if (audioLanguage in ownLanguages) return forcedIndex(subtitles, setOf(audioLanguage)) ?: -1

		val defaultSubtitle = subtitles.firstOrNull { it.index == serverDefault }
		// Vorschlag des Servers behalten, wenn er schon volle Untertitel in der eigenen Sprache sind
		if (defaultSubtitle != null && !isForced(defaultSubtitle) && normalize(defaultSubtitle.language) in ownLanguages) {
			return serverDefault
		}
		alwaysOwn(subtitles.filter { !isForced(it) }, ownLanguages)?.let { return it }

		if (defaultSubtitle == null || isForced(defaultSubtitle)) return serverDefault
		return if (normalize(defaultSubtitle.language) == audioLanguage) forcedIndex(subtitles, setOf(audioLanguage)) ?: -1 else serverDefault
	}

	/** Untertitel in der eigenen Sprache (nicht erzwungen zuerst), sonst null. */
	fun alwaysOwn(subtitles: List<MediaStream>, ownLanguages: Set<String>): Int? {
		val own = subtitles.filter { normalize(it.language) in ownLanguages }
		return (own.firstOrNull { !isForced(it) } ?: own.firstOrNull())?.index
	}

	private val FORCED_TITLE = Regex("forced|erzwungen|nur fremdsprachig", RegexOption.IGNORE_CASE)

	/**
	 * Erzwungene Untertitel (nur fremdsprachige Stellen): Flag in der Datei ODER „Forced“/„erzwungen“/„nur
	 * fremdsprachige“ im Titel – viele Dateien setzen das Flag nicht, nennen die Spur aber so.
	 */
	@JvmStatic
	fun isForced(stream: MediaStream): Boolean =
		stream.isForced == true || listOfNotNull(stream.title, stream.displayTitle).any { FORCED_TITLE.containsMatchIn(it) }

	private fun forcedIndex(subtitles: List<MediaStream>, languages: Set<String>): Int? =
		subtitles.firstOrNull { isForced(it) && normalize(it.language) in languages }?.index

	/** Gründe des Servers fürs Umrechnen (aus der Transcoding-Adresse, z. B. „AudioCodecNotSupported“) – fürs Log. */
	@JvmStatic
	fun transcodeReasons(source: MediaSourceInfo?): String =
		source?.transcodingUrl?.let { Regex("[?&]TranscodeReasons=([^&]*)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
			?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "-"

	private var lastRestartedItem: java.util.UUID? = null

	/** Stream höchstens einmal pro Titel neu starten (verhindert Endlosschleifen). */
	@JvmStatic
	fun allowRestart(item: BaseItemDto?): Boolean {
		if (item == null || item.id == lastRestartedItem) return false
		lastRestartedItem = item.id
		return true
	}

	/** Wahl des Benutzers für die Serie merken (aus oder Sprache). */
	@JvmStatic
	fun remember(item: BaseItemDto?, source: MediaSourceInfo?, index: Int) {
		val seriesId = item?.seriesId ?: return
		val value = if (index < 0) OFF else source?.mediaStreams?.firstOrNull { it.index == index }?.language ?: return
		Timber.i("Untertitel für Serie %s gemerkt: %s", seriesId, value)
		prefs().edit().putString(seriesId.toString(), value).apply()
	}
}
