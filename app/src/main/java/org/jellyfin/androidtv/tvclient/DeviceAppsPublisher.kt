package org.jellyfin.androidtv.tvclient

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.displayPreferencesApi
import org.jellyfin.sdk.model.api.DisplayPreferencesDto
import org.jellyfin.sdk.model.api.ScrollDirection
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Legt die Liste der Apps und Einstellungen dieses Geräts auf dem
 * Jellyfin-Server ab (Anzeige-Einstellungen des Benutzers), damit Jellyfin Remote sie als
 * Knöpfe anzeigen kann.
 *
 * Abruf: GET /DisplayPreferences/tvclient-device-{DeviceId}?client=tvclient
 * → CustomPrefs: apps (JSON [{"id":"com.netflix.ninja","name":"Netflix"}, …]),
 *   settings (z. B. "wifi,bluetooth,display,sound,all"), deviceName, updated.
 *
 * Befehle zurück an den Fernseher: SendString mit "tvclient:app:{id}", "tvclient:settings:{key}"
 * oder "tvclient:open:{bereich}" (siehe [RemoteCommands]).
 */
object DeviceAppsPublisher {
	const val CLIENT = "tvclient"

	@Serializable
	private data class PublishedApp(val id: String, val name: String)

	fun displayPreferencesId(deviceId: String) = "tvclient-device-$deviceId"

	suspend fun publish(context: Context, api: ApiClient) = withContext(Dispatchers.IO) {
		try {
			val deviceId = api.deviceInfo.id
			val id = displayPreferencesId(deviceId)
			val apps = DeviceApps.getApps(context).map { PublishedApp(it.packageName, it.label) }
			val settings = DeviceApps.getSettingsShortcuts(context).joinToString(",") { it.key }

			val current = runCatching {
				api.displayPreferencesApi.getDisplayPreferences(displayPreferencesId = id, client = CLIENT).content
			}.getOrElse { emptyDisplayPreferences() }

			api.displayPreferencesApi.updateDisplayPreferences(
				displayPreferencesId = id,
				client = CLIENT,
				data = current.copy(
					customPrefs = mapOf(
						"apps" to Json.encodeToString(apps),
						"settings" to settings,
						"deviceName" to api.deviceInfo.name,
						"updated" to System.currentTimeMillis().toString(),
					)
				),
			)
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.w(err, "App-Liste konnte nicht an den Server gemeldet werden")
		}
	}

	private fun emptyDisplayPreferences() = DisplayPreferencesDto(
		primaryImageHeight = 0,
		primaryImageWidth = 0,
		customPrefs = emptyMap(),
		rememberIndexing = false,
		scrollDirection = ScrollDirection.HORIZONTAL,
		rememberSorting = false,
		showBackdrop = false,
		showSidebar = false,
		sortOrder = SortOrder.ASCENDING,
	)
}
