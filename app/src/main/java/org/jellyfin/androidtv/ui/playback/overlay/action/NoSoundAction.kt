package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.tvclient.TvClientSound
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

/** Jellyfin-TV-Client: „Kein Ton?“ – an derselben Stelle mit Stereo-AAC neu starten und das für diesen Ausgang merken. */
class NoSoundAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_tvclient_sound)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		TvClientSound.rememberNoSound(context)
		playbackController.refreshStream()
		Toast.makeText(context, R.string.tvclient_no_sound_done, Toast.LENGTH_LONG).show()
	}
}
