package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.tvclient.HomeRow
import org.jellyfin.androidtv.tvclient.TimeFormat
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

/**
 * Jellyfin-TV-Client: Zeilen der Startseite in fester Reihenfolge ein- und ausschalten
 * (statt der zehn Auswahlfelder „Startseitenabschnitt 1–10“).
 */
class HomePreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(userPreferences)

	override val screen by optionsScreen {
		setTitle(R.string.home_prefs)

		category {
			setTitle(R.string.tvclient_home_style)

			checkbox {
				setTitle(R.string.tvclient_home_hero)
				setContent(R.string.tvclient_home_hero_description)
				bind(userPreferences, HomeRow.heroPreference)
			}

			checkbox {
				setTitle(R.string.tvclient_24h)
				setContent(R.string.tvclient_24h_description)
				bind(userPreferences, TimeFormat.use24HourPreference)
			}
		}

		category {
			setTitle(R.string.tvclient_home_rows)

			HomeRow.entries.forEach { row ->
				checkbox {
					setTitle(row.title)
					row.description?.let { setContent(it) }
					bind(userPreferences, row.preference)
				}
			}
		}
	}
}
