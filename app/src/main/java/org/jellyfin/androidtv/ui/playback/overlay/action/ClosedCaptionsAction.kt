package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.tvclient.AudioSubtitleDialog
import org.jellyfin.androidtv.tvclient.TvClientSubtitles
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.ui.playback.setSubtitleIndex
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

class ClosedCaptionsAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var dialog: android.app.Dialog? = null

	init {
		initializeWithIcon(R.drawable.ic_select_subtitle)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		removePopup()

		// Jellyfin-TV-Client: ein Fenster für Ton (links) und Untertitel (rechts)
		val streams = playbackController.currentMediaSource.mediaStreams.orEmpty()
		val audioTracks = playbackController.currentStreamInfo?.selectableAudioStreams.orEmpty()
		val audio = if (audioTracks.size > 1) audioTracks.map {
			AudioSubtitleDialog.Option(it.index, it.displayTitle ?: it.language ?: "#${it.index}", it.index == playbackController.audioStreamIndex)
		} else emptyList()
		val subtitleStreams = streams.filter { it.type == MediaStreamType.SUBTITLE }
		val subtitles = if (subtitleStreams.isEmpty()) emptyList() else
			listOf(AudioSubtitleDialog.Option(-1, context.getString(R.string.lbl_none), playbackController.subtitleStreamIndex == -1)) +
				subtitleStreams.map {
					AudioSubtitleDialog.Option(it.index, it.displayTitle ?: it.language ?: "#${it.index}", it.index == playbackController.subtitleStreamIndex)
				}

		dialog = AudioSubtitleDialog.show(
			context = context,
			audio = audio,
			subtitles = subtitles,
			onAudio = { playbackController.switchAudioStream(it) },
			onSubtitle = { index ->
				// Jellyfin-TV-Client: Wahl für die nächsten Folgen der Serie merken
				TvClientSubtitles.remember(playbackController.currentlyPlayingItem, playbackController.currentMediaSource, index)
				playbackController.setSubtitleIndex(index)
			},
			onDismiss = {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				dialog = null
			},
		)
	}

	fun removePopup() {
		dialog?.dismiss()
	}
}
