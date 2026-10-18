package org.jellyfin.androidtv.tvclient

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Jellyfin-TV-Client: OK im Bildschirmschoner öffnet die Detailseite des gerade gezeigten Titels
 * (wie in der webOS-App); jede andere Taste beendet den Schoner wie bisher.
 */
object ScreensaverDetails {
	/** Titel, der im Bildschirmschoner gerade zu sehen ist. */
	val current = MutableStateFlow<UUID?>(null)

	fun isOk(keyCode: Int) = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
		keyCode == KeyEvent.KEYCODE_ENTER ||
		keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
		keyCode == KeyEvent.KEYCODE_BUTTON_A
}
