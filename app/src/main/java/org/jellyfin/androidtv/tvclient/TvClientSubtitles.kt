package org.jellyfin.androidtv.tvclient

import android.content.Context
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

	/** Sprachcodes, die als „eigene Sprache“ gelten (z. B. de, deu, ger). */
	fun ownLanguages(locale: Locale = Locale.getDefault()): Set<String> = buildSet {
		add(locale.language.lowercase())
		runCatching { add(locale.isO3Language.lowercase()) }
		if (locale.language == "de") addAll(listOf("de", "deu", "ger"))
	}

	/**
	 * Untertitel für den Start eines Titels. Gibt -1 (aus), einen Stream-Index oder den Vorschlag
	 * des Servers zurück.
	 */
	@JvmStatic
	fun chooseSubtitleIndex(item: BaseItemDto?, source: MediaSourceInfo?, serverDefault: Int?, audioIndex: Int): Int? {
		if (item == null || source == null || !enabled()) return serverDefault
		val streams = source.mediaStreams.orEmpty()
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }

		// Für diese Serie gemerkte Wahl
		val remembered = item.seriesId?.let { prefs().getString(it.toString(), null) }
		if (remembered == OFF) return forcedIndex(subtitles, ownLanguages()) ?: -1
		if (remembered != null) {
			subtitles.firstOrNull { it.language.equals(remembered, ignoreCase = true) && it.isForced != true }
				?.let { return it.index }
		}

		return decide(streams, serverDefault, audioIndex, ownLanguages())
	}

	/** Reine Entscheidung (testbar): eigene Tonsprache → keine (nicht erzwungenen) Untertitel. */
	fun decide(streams: List<MediaStream>, serverDefault: Int?, audioIndex: Int, ownLanguages: Set<String>): Int? {
		val subtitles = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val audios = streams.filter { it.type == MediaStreamType.AUDIO }
		val audio = audios.firstOrNull { it.index == audioIndex }
			?: audios.firstOrNull { it.isDefault }
			?: audios.firstOrNull()
		val defaultSubtitle = subtitles.firstOrNull { it.index == serverDefault } ?: return serverDefault

		val audioLanguage = audio?.language?.lowercase()
		return if (audioLanguage != null && audioLanguage in ownLanguages && defaultSubtitle.isForced != true) {
			forcedIndex(subtitles, ownLanguages) ?: -1
		} else {
			serverDefault
		}
	}

	private fun forcedIndex(subtitles: List<MediaStream>, ownLanguages: Set<String>): Int? =
		subtitles.firstOrNull { it.isForced == true && it.language?.lowercase() in ownLanguages }?.index

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
