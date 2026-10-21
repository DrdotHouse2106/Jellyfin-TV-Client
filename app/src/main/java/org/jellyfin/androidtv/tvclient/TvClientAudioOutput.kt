package org.jellyfin.androidtv.tvclient

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioCapabilities
import org.jellyfin.androidtv.constant.Codec
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Darf DTS bzw. TrueHD direkt zum Gerät? Automatisch, in dieser Reihenfolge:
 * 1. Der Ausgang nimmt das Format an (AV-Receiver/Fernseher über HDMI) → durchreichen.
 * 2. Sonst dekodiert die App selbst mit FFmpeg (Mehrkanal-PCM, bei Bedarf Stereo) – kein Umrechnen auf dem Server.
 *    Hersteller-Decoder werden dafür nicht genutzt (siehe HdAudioDecoders), manche bleiben stumm.
 * 3. Nur wenn beides fehlt, wandelt der Server den Ton in Dolby Digital um (5.1 bleibt, Bild direkt).
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
		DtsMode.AUTO -> {
			val passthrough = runCatching { fromEncodings(supportedEncodings(context)) }
				.onFailure { Timber.w(it, "Audio-Ausgang nicht prüfbar") }
				.getOrDefault(emptySet())
			val decodable = runCatching { fromDecoder(::ffmpegSupports) }.getOrDefault(emptySet())
			Timber.i("DTS/TrueHD: durchreichen %s, selbst dekodieren %s", passthrough, decodable)
			passthrough + decodable
		}
	}

	/** Was die App selbst dekodieren kann (testbar: [supports] bekommt den MIME-Typ). */
	fun fromDecoder(supports: (String) -> Boolean): Set<String> = buildSet {
		if (supports(MimeTypes.AUDIO_DTS)) {
			add(Codec.Audio.DCA)
			add(Codec.Audio.DTS)
		}
		if (supports(MimeTypes.AUDIO_TRUEHD)) {
			add(Codec.Audio.TRUEHD)
			add(Codec.Audio.MLP)
		}
	}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	private fun ffmpegSupports(mimeType: String): Boolean =
		androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable() && androidx.media3.decoder.ffmpeg.FfmpegLibrary.supportsFormat(mimeType)

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
