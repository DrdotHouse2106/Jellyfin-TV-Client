package org.jellyfin.androidtv.integration.dream

import android.content.Intent
import android.service.dreams.DreamService
import android.view.KeyEvent
import org.jellyfin.androidtv.integration.dream.composable.DreamHost
import org.jellyfin.androidtv.tvclient.ScreensaverDetails
import org.jellyfin.androidtv.ui.startup.StartupActivity

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamServiceCompat() {
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		// Jellyfin-TV-Client: Tasten selbst auswerten – OK öffnet den gezeigten Titel, jede andere beendet
		isInteractive = true
		isFullscreen = true

		setContent {
			DreamHost()
		}
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (event.action != KeyEvent.ACTION_UP) return true
		val itemId = ScreensaverDetails.current.value
		if (ScreensaverDetails.isOk(event.keyCode) && itemId != null) {
			startActivity(
				Intent(this, StartupActivity::class.java)
					.putExtra(StartupActivity.EXTRA_ITEM_ID, itemId.toString())
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)
		}
		finish()
		return true
	}
}
