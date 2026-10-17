package org.jellyfin.androidtv.tvclient

import androidx.annotation.StringRes
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.booleanPreference

/**
 * Jellyfin-TV-Client: Zeilen der Startseite in fester Reihenfolge. Jede Zeile lässt sich unter
 * Einstellungen → Startseite ein- und ausschalten (gespeichert auf dem Gerät).
 * Ersetzt die zehn Auswahlfelder „Startseitenabschnitt 1–10“ der offiziellen App.
 */
enum class HomeRow(
	key: String,
	@StringRes val title: Int,
	enabledByDefault: Boolean,
	@StringRes val description: Int? = null,
) {
	RESUME("resume", R.string.home_section_resume, true),
	NEXT_UP("nextup", R.string.home_section_next_up, true),
	RECOMMENDED_MOVIES("localrecs_movies", R.string.lbl_localrecs_movies, true, R.string.tvclient_home_requires_plugin),
	RECOMMENDED_SERIES("localrecs_series", R.string.lbl_localrecs_series, true, R.string.tvclient_home_requires_plugin),
	RECOMMENDED_LIVE_TV("localrecs_livetv", R.string.lbl_localrecs_livetv, true, R.string.tvclient_home_requires_plugin),
	LIVE_TV("livetv", R.string.home_section_livetv, false, R.string.tvclient_home_livetv_description),
	LATEST_MEDIA("latest", R.string.home_section_latest_media, true),
	WEB_SECTIONS("web_sections", R.string.tvclient_home_web_sections, true, R.string.tvclient_home_web_sections_description),
	GENRES("genres", R.string.tvclient_home_genres, true, R.string.tvclient_home_genres_description),
	RESUME_AUDIO("resume_audio", R.string.home_section_resume_audio, false),
	ACTIVE_RECORDINGS("recordings", R.string.home_section_active_recordings, false),
	APPS("apps", R.string.tvclient_row_apps, false, R.string.tvclient_home_apps_description),
	DEVICE("device", R.string.tvclient_row_device, false, R.string.tvclient_home_device_description),
	LIBRARIES("libraries", R.string.home_section_library, true),
	;

	val preference = booleanPreference("pref_tvclient_home_$key", enabledByDefault)

	companion object {
		/** Netflix-Ansicht: großer Infobereich zum ausgewählten Titel oben auf der Startseite. */
		val heroPreference = booleanPreference("pref_tvclient_home_hero", true)

		/** Eingeschaltete Zeilen in der festen Reihenfolge. */
		fun enabled(userPreferences: UserPreferences): List<HomeRow> =
			entries.filter { userPreferences[it.preference] }
	}
}
