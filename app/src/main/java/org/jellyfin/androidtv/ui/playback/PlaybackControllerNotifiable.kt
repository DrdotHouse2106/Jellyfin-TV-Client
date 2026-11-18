package org.jellyfin.androidtv.ui.playback

interface PlaybackControllerNotifiable {
	fun onCompletion()
	fun onError()
	fun onPrepared()
	fun onProgress()
	fun onPlaybackSpeedChange(newSpeed: Float)

	/** Jellyfin-TV-Client: Hänger mit gefülltem Puffer – Decoder läuft nach einem Sprung nicht an. */
	fun onDecoderStall() = onError()
}
