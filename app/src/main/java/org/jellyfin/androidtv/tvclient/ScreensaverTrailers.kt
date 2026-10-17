package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.preference.booleanPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Jellyfin-TV-Client: Trailer im Bildschirmschoner (stumm). Nur Trailer, die auf dem Server liegen;
 * Titel ohne Trailer werden wie bisher als Bild gezeigt.
 */
object ScreensaverTrailers {
	val enabledPreference = booleanPreference("pref_tvclient_screensaver_trailers", false)

	private val defaultDuration = 120.seconds
	private val maxDuration = 180.seconds

	data class Trailer(val url: String, val duration: Duration?)

	suspend fun trailer(api: ApiClient, item: BaseItemDto): Trailer? {
		if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.SERIES) return null
		return runCatching {
			val local = withContext(Dispatchers.IO) { api.userLibraryApi.getLocalTrailers(item.id).content.firstOrNull() }
				?: return null
			val url = api.createUrl(
				pathTemplate = "/Videos/{itemId}/stream",
				pathParameters = mapOf("itemId" to local.id),
				queryParameters = mapOf("static" to true, "api_key" to api.accessToken),
			)
			Trailer(url, local.runTimeTicks?.ticks)
		}.getOrNull()
	}

	/** Wie lange ein Titel mit Trailer stehen bleibt: Trailerlänge plus kurzer Nachlauf, höchstens 3 Minuten. */
	fun showDuration(duration: Duration?): Duration = minOf((duration ?: defaultDuration) + 3.seconds, maxDuration)
}
