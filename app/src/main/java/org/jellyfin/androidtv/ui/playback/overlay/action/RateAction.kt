package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.tvclient.Ratings
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent

/** Jellyfin-TV-Client: Daumen hoch/runter im Player (Folgen bewerten ihre Serie). */
class RateAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.tvclient_ic_thumb_up)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		val item = playbackController.currentlyPlayingItem ?: return
		val target = Ratings.target(item) ?: return
		val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
		val scope = view.findViewTreeLifecycleOwner()?.lifecycleScope ?: return

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		scope.launch {
			val current = Ratings.get(api, target)
			dismissPopup()
			popup = PopupMenu(context, view, Gravity.END).apply {
				menu.add(0, LIKE, 0, R.string.tvclient_like).isChecked = current == true
				menu.add(0, DISLIKE, 1, R.string.tvclient_dislike).isChecked = current == false
				menu.add(0, NONE, 2, R.string.tvclient_rating_none).isChecked = current == null
				menu.setGroupCheckable(0, true, true)
				setOnDismissListener {
					videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
					popup = null
				}
				setOnMenuItemClickListener { menuItem ->
					val likes = when (menuItem.itemId) {
						LIKE -> true
						DISLIKE -> false
						else -> null
					}
					scope.launch {
						runCatching { Ratings.set(api, target, likes) }
							.onSuccess { Toast.makeText(context, R.string.tvclient_rating_saved, Toast.LENGTH_SHORT).show() }
					}
					true
				}
			}
			popup?.show()
		}
	}

	fun dismissPopup() {
		popup?.dismiss()
	}

	private companion object {
		const val LIKE = 1
		const val DISLIKE = 2
		const val NONE = 3
	}
}
