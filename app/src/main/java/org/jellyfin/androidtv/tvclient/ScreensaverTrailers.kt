package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Jellyfin-TV-Client: Teaser bzw. Trailer im Bildschirmschoner. Nur Videos, die auf dem Server liegen;
 * Titel ohne Trailer werden wie bisher als Bild gezeigt.
 */
object ScreensaverTrailers {
	val enabledPreference = booleanPreference("pref_tvclient_screensaver_trailers", false)

	/** Lautstärke der Teaser im Schoner in Prozent: 0 = stumm (Standard), 30 = leise, 75 = normal. */
	val volumePreference = intPreference("pref_tvclient_screensaver_volume", 0)

	private val defaultDuration = 120.seconds
	private val maxDuration = 180.seconds

	data class Trailer(val url: String, val duration: Duration?)

	/**
	 * Video für den Bildschirmschoner: ein **Teaser** vom Server, wenn es einen gibt (kurz, kaum
	 * Spoiler), sonst der erste Trailer. Teaser erkennt die App am Namen bzw. Dateipfad („Teaser“)
	 * oder an der Länge (bis 100 Sekunden), unter Trailern und Extras.
	 */
	suspend fun trailer(api: ApiClient, item: BaseItemDto): Trailer? {
		if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.SERIES) return null
		return runCatching {
			val (trailers, extras) = withContext(Dispatchers.IO) {
				val trailers = api.userLibraryApi.getLocalTrailers(item.id).content
				val extras = runCatching { api.userLibraryApi.getSpecialFeatures(item.id).content }.getOrDefault(emptyList())
				trailers to extras
			}
			val video = pickTeaser(trailers + extras) ?: trailers.firstOrNull() ?: return null
			val url = api.createUrl(
				pathTemplate = "/Videos/{itemId}/stream",
				pathParameters = mapOf("itemId" to video.id),
				// Anmeldung per Header (ServerAuth), nicht in der Adresse
				queryParameters = mapOf("static" to true),
			)
			val duration = video.runTimeTicks?.ticks
			if (duration == null || duration < minDuration) {
				ClientDiagnostics.report("Bildschirmschoner: Trailer „${video.name}“ (${video.id}) zu „${item.name}“ ohne brauchbare Länge ($duration) – zeige ihn mit Standardlänge")
			}
			Trailer(url, duration)
		}.getOrNull()
	}

	/** Teaser auswählen (testbar): Name/Pfad mit „Teaser“ zuerst, sonst ein kurzes Video. */
	fun pickTeaser(videos: List<BaseItemDto>): BaseItemDto? {
		fun BaseItemDto.isTeaser() = listOfNotNull(name, path).any { it.contains("teaser", ignoreCase = true) }
		return videos.firstOrNull { it.isTeaser() }
			?: videos.filter { (it.runTimeTicks?.ticks ?: maxTeaser) < maxTeaser }.minByOrNull { it.runTimeTicks ?: Long.MAX_VALUE }
	}

	private val maxTeaser = 100.seconds

	/** Unter dieser Länge ist die Angabe unbrauchbar (z. B. frisch ersetzte Trailer-Datei, noch nicht neu eingelesen). */
	private val minDuration = 15.seconds

	/**
	 * Wie lange ein Titel mit Trailer stehen bleibt: Trailerlänge plus kurzer Nachlauf, höchstens 3 Minuten.
	 * Fehlt die Länge oder ist sie unplausibel kurz, gilt der Standard – sonst sprang der Schoner schon nach
	 * 2–3 Sekunden zum nächsten Titel.
	 */
	fun showDuration(duration: Duration?): Duration {
		val usable = duration?.takeIf { it >= minDuration } ?: defaultDuration
		return minOf(usable + 3.seconds, maxDuration)
	}
}
