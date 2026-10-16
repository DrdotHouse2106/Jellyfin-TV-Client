package org.jellyfin.androidtv.tvclient

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.koin.java.KoinJavaComponent

/**
 * Jellyfin-TV-Client: Intro und Abspann wie bei Netflix – „Überspringen“ erscheint mit einem
 * Countdown und springt danach automatisch, außer man drückt „Zurück“.
 * Gilt für Abschnitte, die auf „Nachfragen“ stehen (Einstellungen → Wiedergabe).
 */
object TvClientSkip {
	const val COUNTDOWN_SECONDS = 5

	val autoSkipPreference = booleanPreference("pref_tvclient_skip_countdown", true)

	fun autoSkipEnabled(): Boolean = runCatching {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[autoSkipPreference]
	}.getOrDefault(true)
}
