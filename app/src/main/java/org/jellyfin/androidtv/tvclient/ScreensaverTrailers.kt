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
	 * Video für den Bildschirmschoner: nur **echte Trailer** des Titels (LocalTrailers bzw. Extras vom Typ
	 * Trailer) – nie andere Extras wie Making-of, entfallene Szenen oder kurze Clips von DVDs.
	 */
	suspend fun trailer(api: ApiClient, item: BaseItemDto): Trailer? {
		if (item.type != BaseItemKind.MOVIE && item.type != BaseItemKind.SERIES) return null
		return runCatching {
			val candidates = withContext(Dispatchers.IO) {
				val trailers = api.userLibraryApi.getLocalTrailers(item.id).content
				val extraTrailers = runCatching { api.userLibraryApi.getSpecialFeatures(item.id).content }.getOrDefault(emptyList())
					.filter { it.extraType == org.jellyfin.sdk.model.api.ExtraType.TRAILER }
				(trailers + extraTrailers).distinctBy { it.id }
			}
			val video = pickTrailer(candidates)
			if (video == null) {
				if (candidates.isNotEmpty()) ClientDiagnostics.report("Bildschirmschoner: zu „${item.name}“ nur zu kurze Trailer (${candidates.joinToString { "${it.name} ${it.runTimeTicks?.ticks}" }}) – zeige das Bild")
				return null
			}
			val url = api.createUrl(
				pathTemplate = "/Videos/{itemId}/stream",
				pathParameters = mapOf("itemId" to video.id),
				// Anmeldung per Header (ServerAuth), nicht in der Adresse
				queryParameters = mapOf("static" to true),
			)
			Trailer(url, video.runTimeTicks?.ticks)
		}.getOrNull()
	}

	/**
	 * Trailer auswählen (testbar): Clips unter 15 Sekunden fallen weg, „Trailer“ im Namen geht vor „Teaser“,
	 * sonst der längste. Videos ohne Längenangabe (z. B. gerade neu eingelesen) gelten als brauchbar.
	 */
	fun pickTrailer(videos: List<BaseItemDto>): BaseItemDto? {
		fun BaseItemDto.named(word: String) = listOfNotNull(name, path).any { it.contains(word, ignoreCase = true) }
		return videos
			.filter { video -> video.runTimeTicks?.ticks?.let { it >= minDuration } ?: true }
			.sortedWith(
				compareByDescending<BaseItemDto> { it.named("trailer") && !it.named("teaser") }
					.thenByDescending { it.runTimeTicks ?: 0L }
			)
			.firstOrNull()
	}

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
