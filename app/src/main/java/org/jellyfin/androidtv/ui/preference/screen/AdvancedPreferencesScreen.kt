package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

/**
 * Jellyfin-TV-Client: selten gebrauchte Einstellungen (Absturzberichte, Entwickleroptionen).
 */
class AdvancedPreferencesScreen : OptionsFragment() {
	override val screen by optionsScreen {
		setTitle(R.string.advanced_settings)

		category {
			link {
				setTitle(R.string.pref_playback_advanced)
				icon = R.drawable.ic_next
				withFragment<PlaybackAdvancedPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_telemetry_category)
				setContent(R.string.pref_telemetry_description)
				icon = R.drawable.ic_error
				withFragment<CrashReportingPreferencesScreen>()
			}

			link {
				setTitle(R.string.pref_developer_link)
				setContent(R.string.pref_developer_link_description)
				icon = R.drawable.ic_flask
				withFragment<DeveloperPreferencesScreen>()
			}
		}
	}
}
