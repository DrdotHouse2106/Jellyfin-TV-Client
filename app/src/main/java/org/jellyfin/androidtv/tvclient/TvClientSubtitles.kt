package org.jellyfin.androidtv.tvclient

import android.content.Context
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
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

	private const val OFF = "off"
	private const val PREFS = "tvclient_subtitles"

	private fun enabled(): Boolean = runCatching {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[smartPreference]
	}.getOrDefault(true)

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
		if (item == null || source == null || !enabled()) return serverDefault
		val streams = source.mediaStreams.orEmpty()
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val own = ownLanguages()

		// Für diese Serie gemerkte Wahl
		val remembered = item.seriesId?.let { prefs().getString(it.toString(), null) }
		if (remembered == OFF) return forcedIndex(subtitles, setOfNotNull(audioLanguage(streams, audioIndex)) + own) ?: -1
		// Gemerkte Untertitel in der Sprache des Tons ignorieren (deutscher Ton + deutsche Untertitel)
		if (remembered != null && normalize(remembered) != audioLanguage(streams, audioIndex)) {
			subtitles.firstOrNull { normalize(it.language) == normalize(remembered) && it.isForced != true }
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
	 * Reine Entscheidung (testbar): Keine (nicht erzwungenen) Untertitel, wenn der Ton in der eigenen
	 * Sprache läuft oder die Untertitel dieselbe Sprache wie der Ton haben.
	 */
	fun decide(streams: List<MediaStream>, serverDefault: Int?, audioIndex: Int?, ownLanguages: Set<String>): Int? {
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val defaultSubtitle = subtitles.firstOrNull { it.index == serverDefault } ?: return serverDefault
		if (defaultSubtitle.isForced == true) return serverDefault

		val audioLanguage = audioLanguage(streams, audioIndex) ?: return serverDefault
		val subtitleLanguage = normalize(defaultSubtitle.language)
		return if (audioLanguage in ownLanguages || audioLanguage == subtitleLanguage) {
			forcedIndex(subtitles, setOf(audioLanguage)) ?: -1
		} else {
			serverDefault
		}
	}

	private fun forcedIndex(subtitles: List<MediaStream>, languages: Set<String>): Int? =
		subtitles.firstOrNull { it.isForced == true && normalize(it.language) in languages }?.index

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
