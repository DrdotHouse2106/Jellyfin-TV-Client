package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import org.jellyfin.androidtv.tvclient.HomeRow
import org.jellyfin.androidtv.tvclient.Jellyseerr
import org.jellyfin.androidtv.tvclient.TimeFormat
import org.jellyfin.androidtv.tvclient.TrailerPreview
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
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

			checkbox {
				setTitle(R.string.tvclient_trailer_preview)
				setContent(R.string.tvclient_trailer_preview_description)
				bind(userPreferences, TrailerPreview.enabledPreference)
			}

			checkbox {
				setTitle(R.string.tvclient_trailer_sound)
				bind(userPreferences, TrailerPreview.soundPreference)
				depends { userPreferences[TrailerPreview.enabledPreference] }
			}
		}

		// Jellyfin-TV-Client: Filme/Serien bei Jellyseerr anfragen (Symbol in der Navigation links)
		category {
			setTitle(R.string.tvclient_jellyseerr_title)

			checkbox {
				setTitle(R.string.tvclient_jellyseerr_enable)
				setContent(R.string.tvclient_jellyseerr_enable_description)
				bind(userPreferences, Jellyseerr.enabledPreference)
			}

			action {
				setTitle(R.string.tvclient_jellyseerr_url)
				content = userPreferences[Jellyseerr.urlPreference].ifBlank { getString(R.string.tvclient_jellyseerr_not_set) }
				onActivate = {
					editText(R.string.tvclient_jellyseerr_url, userPreferences[Jellyseerr.urlPreference], getString(R.string.tvclient_jellyseerr_url_hint)) {
						userPreferences[Jellyseerr.urlPreference] = Jellyseerr.normalizeUrl(it)
					}
				}
				depends { userPreferences[Jellyseerr.enabledPreference] }
			}

			action {
				setTitle(R.string.tvclient_jellyseerr_api_key)
				setContent(R.string.tvclient_jellyseerr_api_key_description)
				onActivate = {
					editText(R.string.tvclient_jellyseerr_api_key, userPreferences[Jellyseerr.apiKeyPreference], null) {
						userPreferences[Jellyseerr.apiKeyPreference] = it.trim()
					}
				}
				depends { userPreferences[Jellyseerr.enabledPreference] }
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

	/** Einfacher Eingabedialog für Text (Adresse, API-Schlüssel). */
	private fun editText(title: Int, value: String, hint: String?, onSave: (String) -> Unit) {
		val input = EditText(requireContext()).apply {
			setText(value)
			setSingleLine()
			inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
			hint?.let { this.hint = it }
		}
		AlertDialog.Builder(requireContext())
			.setTitle(title)
			.setView(input)
			.setPositiveButton(R.string.lbl_ok) { _, _ ->
				onSave(input.text.toString())
				Toast.makeText(requireContext(), R.string.tvclient_saved, Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton(R.string.lbl_cancel, null)
			.show()
	}
}
