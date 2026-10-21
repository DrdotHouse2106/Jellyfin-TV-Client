package org.jellyfin.androidtv.tvclient

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.audio.AudioCapabilities
import org.jellyfin.androidtv.constant.Codec
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Darf DTS bzw. TrueHD direkt zum Gerät? Manche Sticks geben DTS ohne Ton aus, weil
 * ihr HDMI-Ausgang nur Dolby Digital kann. Dann soll der Server nur den Ton umwandeln (5.1 bleibt).
 */
object TvClientAudioOutput {
	enum class DtsMode { AUTO, ON, OFF }

	val HD_AUDIO_CODECS = setOf(Codec.Audio.DCA, Codec.Audio.DTS, Codec.Audio.TRUEHD, Codec.Audio.MLP)

	/**
	 * Erlaubte DTS-/TrueHD-Codecs für das Geräteprofil. Automatisch: Fähigkeiten des aktuellen Ausgangs
	 * (Fernseher oder AV-Receiver über HDMI) – bei jedem Wiedergabestart neu abgefragt, nicht gespeichert.
	 */
	fun directHdAudio(context: Context, mode: DtsMode): Set<String> = when (mode) {
		DtsMode.ON -> HD_AUDIO_CODECS
		DtsMode.OFF -> emptySet()
		DtsMode.AUTO -> runCatching { fromEncodings(supportedEncodings(context)) }
			.onFailure { Timber.w(it, "Audio-Ausgang nicht prüfbar – DTS/TrueHD über den Server") }
			.getOrDefault(emptySet())
	}

	/** Aus den vom Ausgang angenommenen Formaten die direkt erlaubten Codecs bestimmen. */
	fun fromEncodings(encodings: Set<Int>): Set<String> = buildSet {
		if (C.ENCODING_DTS in encodings || C.ENCODING_DTS_HD in encodings) {
			add(Codec.Audio.DCA)
			add(Codec.Audio.DTS)
		}
		if (C.ENCODING_DOLBY_TRUEHD in encodings) {
			add(Codec.Audio.TRUEHD)
			add(Codec.Audio.MLP)
		}
	}

	@Suppress("DEPRECATION")
	private fun supportedEncodings(context: Context): Set<Int> {
		val capabilities = AudioCapabilities.getCapabilities(context, AudioAttributes.DEFAULT, null)
		val encodings = listOf(C.ENCODING_DTS, C.ENCODING_DTS_HD, C.ENCODING_DOLBY_TRUEHD).filter { capabilities.supportsEncoding(it) }.toSet()
		Timber.i("Audio-Ausgang: %s (DTS=%s, DTS-HD=%s, TrueHD=%s)", capabilities, C.ENCODING_DTS in encodings, C.ENCODING_DTS_HD in encodings, C.ENCODING_DOLBY_TRUEHD in encodings)
		return encodings
	}
}
