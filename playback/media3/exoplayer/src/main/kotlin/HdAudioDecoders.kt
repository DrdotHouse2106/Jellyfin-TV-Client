@file:OptIn(UnstableApi::class)

package org.jellyfin.playback.media3.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Jellyfin-TV-Client: DTS und TrueHD nie über Decoder des Geräteherstellers. Manche Sticks (z. B. Amlogic) melden
 * DTS-Decoder, geben damit aber keinen Ton aus. Kann der Ausgang DTS/TrueHD selbst (AV-Receiver), reicht ExoPlayer
 * weiter durch – sonst dekodiert FFmpeg in der App (Mehrkanal-PCM, Android mischt bei Bedarf auf Stereo herunter).
 */
object HdAudioDecoders {
	val MIME_TYPES = setOf(
		MimeTypes.AUDIO_DTS,
		MimeTypes.AUDIO_DTS_HD,
		MimeTypes.AUDIO_DTS_EXPRESS,
		MimeTypes.AUDIO_DTS_X,
		MimeTypes.AUDIO_TRUEHD,
	)

	@JvmStatic
	val selector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
		if (mimeType in MIME_TYPES) emptyList()
		else MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
	}
}
