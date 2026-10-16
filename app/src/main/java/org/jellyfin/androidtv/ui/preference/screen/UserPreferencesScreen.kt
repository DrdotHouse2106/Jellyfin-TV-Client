package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.category.aboutCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

/**
 * Jellyfin-TV-Client: Einstellungen neu geordnet – Wichtiges zuerst, Selten-Gebrauchtes unter „Erweitert“.
 */
class UserPreferencesScreen : OptionsFragment() {
	override val screen by optionsScreen {
		setTitle(R.string.settings_title)

		category {
			link {
				setTitle(R.string.home_prefs)
				setContent(R.string.tvclient_settings_home_description)
				icon = R.drawable.ic_house
				withFragment<HomePreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_playback)
				setContent(R.string.tvclient_settings_playback_description)
				icon = R.drawable.ic_next
				withFragment<PlaybackPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_libraries)
				setContent(R.string.tvclient_settings_libraries_description)
				icon = R.drawable.ic_grid
				withFragment<LibrariesPreferencesScreen>()
			}

			link {
				setTitle(R.string.tvclient_settings_appearance)
				setContent(R.string.tvclient_settings_appearance_description)
				icon = R.drawable.ic_adjust
				withFragment<CustomizationPreferencesScreen>()
			}

			link {
				setTitle(R.string.tvclient_settings_account)
				setContent(R.string.pref_login_description)
				icon = R.drawable.ic_users
				withFragment<AuthPreferencesScreen>()
			}

			link {
				setTitle(R.string.advanced_settings)
				setContent(R.string.tvclient_settings_advanced_description)
				icon = R.drawable.ic_flask
				withFragment<AdvancedPreferencesScreen>()
			}
		}

		aboutCategory()
	}
}
