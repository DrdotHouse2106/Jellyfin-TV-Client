package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.text.format.DateFormat
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference
import org.koin.java.KoinJavaComponent

/**
 * Jellyfin-TV-Client: Uhrzeiten im 24-Stunden-Format („23:03“ statt „11:03 PM“), unabhängig von der
 * Systemeinstellung des Fernsehers. Abschaltbar unter Einstellungen → Startseite → Darstellung.
 */
object TimeFormat {
	val use24HourPreference = booleanPreference("pref_tvclient_24h", true)

	fun use24Hour(context: Context): Boolean {
		val forced = runCatching {
			KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[use24HourPreference]
		}.getOrDefault(true)
		return forced || DateFormat.is24HourFormat(context)
	}

	/** Uhr-Anzeigen (TextClock) wie auf der Startseite: „20:21“ statt „8:21“, auch bei 12-Stunden-System. */
	@JvmStatic
	fun apply(clock: android.widget.TextClock) {
		if (!use24Hour(clock.context)) return
		clock.format12Hour = "k:mm"
		clock.format24Hour = "k:mm"
	}
}
