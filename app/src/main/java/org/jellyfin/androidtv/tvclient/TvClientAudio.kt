package org.jellyfin.androidtv.tvclient

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.stringPreference
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Tonspur beim Start (wie in der webOS-App).
 * - „own“ (Standard): eine Spur in der eigenen Sprache, wenn der Vorschlag des Servers eine andere Sprache hat.
 * - „original“: Originalton – die erste Spur in fremder Sprache (ohne Kommentarspuren).
 * - „server“: der Vorschlag des Servers wie bisher.
 * Eine früher von Hand gewählte Sprache hat immer Vorrang.
 */
object TvClientAudio {
	const val OWN = "own"
	const val ORIGINAL = "original"
	const val SERVER = "server"

	val modePreference = stringPreference("pref_tvclient_audio_mode", OWN)

	private val commentary = Regex("kommentar|commentary", RegexOption.IGNORE_CASE)

	private fun mode(): String = runCatching {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[modePreference]
	}.getOrDefault(OWN)

	/** Tonspur für den Start, oder null, wenn der Vorschlag des Servers bleibt. */
	@JvmStatic
	fun chooseAudioIndex(source: MediaSourceInfo?, suggested: Int?): Int? {
		val streams = source?.mediaStreams ?: return null
		val result = decide(streams, suggested, mode(), TvClientSubtitles.ownLanguages())
		Timber.i("Ton beim Start: Modus=%s Vorschlag=%s -> %s", mode(), suggested, result)
		return result
	}

	/** Reine Entscheidung (testbar). */
	fun decide(streams: List<MediaStream>, suggested: Int?, mode: String, ownLanguages: Set<String>): Int? {
		val audios = streams.filter { it.type == MediaStreamType.AUDIO }
		if (audios.size < 2) return null
		val current = audios.firstOrNull { it.index == suggested } ?: audios.firstOrNull { it.isDefault == true } ?: audios.first()
		fun isOwn(stream: MediaStream) = TvClientSubtitles.normalize(stream.language) in ownLanguages
		fun isCommentary(stream: MediaStream) = commentary.containsMatchIn(listOfNotNull(stream.title, stream.displayTitle).joinToString(" "))
		val pick = when (mode) {
			ORIGINAL -> if (!isOwn(current) && !isCommentary(current)) null else {
				val foreign = audios.filter { it.language != null && !isOwn(it) && !isCommentary(it) }
				foreign.firstOrNull { it.isDefault == true } ?: foreign.firstOrNull()
			}
			OWN -> if (isOwn(current)) null else {
				val own = audios.filter { isOwn(it) && !isCommentary(it) }
				own.firstOrNull { it.isDefault == true } ?: own.firstOrNull()
			}
			else -> null
		}
		return pick?.index?.takeIf { it != current.index }
	}
}
