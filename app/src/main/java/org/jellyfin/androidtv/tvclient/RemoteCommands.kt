package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Zusatzbefehle vom Handy (z. B. Jellyfin Remote). Sie kommen als
 * Jellyfin-Befehl „SendString“ mit dem Präfix [PREFIX]:
 *
 * - `tvclient:app:{paketname}` – App starten (z. B. `tvclient:app:com.netflix.ninja`)
 * - `tvclient:settings:{wifi|bluetooth|display|sound|all}` – Geräte-Einstellungen öffnen
 * - `tvclient:open:{home|search|settings|guide|recordings|schedule}` – Bereich der App öffnen
 */
object RemoteCommands {
	const val PREFIX = "tvclient:"

	fun isCommand(text: String) = text.startsWith(PREFIX, ignoreCase = true)

	suspend fun handle(
		context: Context,
		navigationRepository: NavigationRepository,
		text: String,
	) = withContext(Dispatchers.Main) {
		val parts = text.substring(PREFIX.length).split(':', limit = 2)
		val type = parts.getOrNull(0)?.lowercase()
		val value = parts.getOrNull(1)?.trim().orEmpty()

		when (type) {
			"app" -> if (!DeviceApps.launch(context, value)) Timber.w("App %s nicht gefunden", value)
			"settings" -> DeviceApps.SettingsShortcut.fromKey(value)?.let { DeviceApps.openSettings(context, it) }
			"open" -> when (value.lowercase()) {
				"home" -> navigationRepository.reset(Destinations.home, true)
				"search" -> navigationRepository.navigate(Destinations.search())
				"guide" -> navigationRepository.navigate(Destinations.liveTvGuide)
				"recordings" -> navigationRepository.navigate(Destinations.liveTvRecordings)
				"schedule" -> navigationRepository.navigate(Destinations.liveTvSchedule)
				"settings" -> context.startActivity(
					ActivityDestinations.userPreferences(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				)

				else -> Timber.w("Unbekannter Bereich %s", value)
			}

			else -> Timber.w("Unbekannter Befehl %s", text)
		}
	}
}
