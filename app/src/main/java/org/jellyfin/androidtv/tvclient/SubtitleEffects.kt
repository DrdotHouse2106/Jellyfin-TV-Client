package org.jellyfin.androidtv.tvclient

import android.app.ActivityManager
import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference

/**
 * Jellyfin-TV-Client: ASS/SSA-Untertitel mit allen Effekten (libass: Schriften aus der MKV, Positionen, Signs,
 * Karaoke, Animationen) oder als einfacher Text (Media3, wie bis 1.40.2). Beides ohne Einbrennen auf dem Server.
 */
object SubtitleEffects {
	enum class Mode { AUTO, ON, OFF }

	val modePreference = enumPreference("pref_tvclient_subtitle_effects", Mode.AUTO)

	/** „Automatisch“ hat auf diesem Gerät beim Abspielen zu viele Bilder verloren – dann einfacher Text. */
	val autoDisabledPreference = booleanPreference("pref_tvclient_subtitle_effects_auto_off", false)

	/** So viele verlorene Bilder (bei laufenden Effekt-Untertiteln) gelten als „zu schwach“. */
	const val DROPPED_FRAMES_LIMIT = 60

	/** Effekte für diese Wiedergabe einschalten? */
	@JvmStatic
	fun enabled(context: Context, prefs: UserPreferences): Boolean = when (prefs[modePreference]) {
		Mode.ON -> true
		Mode.OFF -> false
		Mode.AUTO -> !prefs[autoDisabledPreference] && !lowRam(context)
	}

	private fun lowRam(context: Context): Boolean = runCatching {
		(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
	}.getOrDefault(false)

	/** Automatisch auf diesem Gerät abschalten (einmal) und den Grund ins Client-Log schreiben. */
	@JvmStatic
	fun disableAuto(prefs: UserPreferences, droppedFrames: Int) {
		if (prefs[modePreference] != Mode.AUTO || prefs[autoDisabledPreference]) return
		prefs[autoDisabledPreference] = true
		ClientDiagnostics.report("Untertitel-Effekte: $droppedFrames verlorene Bilder – „Automatisch“ zeigt auf diesem Gerät ab jetzt einfachen Text")
	}
}
