package org.jellyfin.androidtv.tvclient

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Jellyfin-TV-Client: Hinweis auf kommende und heute neue Folgen auf der Serienseite wie im Web
 * (GET /LocalRecs/Upcoming/Series/{id}, Plugin Local Recommendations ab 12.52, mit Sonarr).
 */
object SeriesUpcoming {
	/**
	 * [next]: „Staffelstart morgen · 19:00 Uhr · Paramount+ · S03E01 Der Anfang · +2 weitere bis 07.11.“,
	 * [newText]: „Neu: S02E05“, [newIds]: heute erschienene, vorhandene Folgen (Id ohne Bindestriche).
	 */
	data class Info(val next: String?, val newText: String?, val newIds: Set<String>)

	private val json = Json { ignoreUnknownKeys = true }

	/** Heute erschienene Folgen aller zuletzt geöffneten Serien – die Karten tragen dann „NEU“. */
	private val newIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

	private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

	/** Antwort des Endpunkts (testbar); null bei 204, leerer Antwort oder ohne Inhalt. */
	fun parse(text: String): Info? {
		if (text.isBlank()) return null
		val obj = json.parseToJsonElement(text) as? JsonObject ?: return null
		val next = listOfNotNull(obj.text("Text"), obj.text("MoreText")).joinToString(" · ").takeIf { it.isNotEmpty() }
		val ids = (obj["NewToday"] as? JsonArray).orEmpty().mapNotNull { entry ->
			(entry as? JsonObject)?.text("ItemId")?.replace("-", "")?.lowercase()
		}.toSet()
		val newText = obj.text("NewText")
		return if (next == null && newText == null && ids.isEmpty()) null else Info(next, newText, ids)
	}

	/** Ist die Folge heute erschienen? (für die Markierung auf der Karte) */
	@JvmStatic
	fun isNew(item: BaseItemDto?): Boolean =
		item != null && item.type == BaseItemKind.EPISODE && newIds.isNotEmpty() && TvClientSections.key(item.id) in newIds

	/** Serienseite: Hinweis laden; ohne Plugin, ohne Sonarr oder ohne Termin passiert nichts. */
	@JvmStatic
	fun load(lifecycle: Lifecycle, seriesId: UUID, onInfo: (Info) -> Unit) {
		lifecycle.coroutineScope.launch {
			val info = withContext(Dispatchers.IO) {
				runCatching {
					val (code, body) = TvClientBehavior.call(
						"GET",
						"/LocalRecs/Upcoming/Series/${TvClientSections.key(seriesId)}?utcOffsetMinutes=${TvClientBehavior.utcOffsetMinutes()}${TvClientBehavior.timeZoneParameter()}",
					)
					if (code == 200) parse(body) else null
				}.onFailure { Timber.w(it, "Kommende Folgen nicht verfügbar") }.getOrNull()
					?: fromJellyfin(seriesId)
			} ?: return@launch
			newIds.addAll(info.newIds)
			onInfo(info)
		}
	}

	/**
	 * Ohne Plugin bzw. Sonarr: nächste angekündigte Folge aus Jellyfin selbst (virtuelle Folge) als Hinweis,
	 * z. B. „Neue Folge am Fr. 30.10. · S01E18 Titel“ – statt einer Zeile mit leeren Karten.
	 */
	private suspend fun fromJellyfin(seriesId: UUID): Info? = runCatching {
		val api = org.koin.java.KoinJavaComponent.get<org.jellyfin.sdk.api.client.ApiClient>(org.jellyfin.sdk.api.client.ApiClient::class.java)
		val next = api.tvShowsApi.getUpcomingEpisodes(parentId = seriesId, limit = 1).content.items.firstOrNull()
		next?.let { nextEpisodeHint(it) }?.let { Info(it, null, emptySet()) }
	}.getOrNull()

	/** „Neue Folge am Fr. 30.10. · S01E18 Titel“ (testbar); das SDK liefert Termine schon in Gerätezeit. */
	fun nextEpisodeHint(episode: BaseItemDto, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String? {
		val date = episode.premiereDate ?: return null
		val local = date.atZone(java.time.ZoneId.systemDefault()).withZoneSameInstant(zone)
		val day = local.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd.MM.", java.util.Locale.GERMAN))
		val code = if (episode.parentIndexNumber != null && episode.indexNumber != null) "S%02dE%02d".format(episode.parentIndexNumber, episode.indexNumber) else null
		val title = listOfNotNull(code, episode.name?.takeIf { it.isNotBlank() }).joinToString(" ")
		return listOf("Neue Folge am $day", title).filter { it.isNotBlank() }.joinToString(" · ")
	}

	/** Termin um Mitternacht UTC = Platzhalter ohne Uhrzeit (testbar); [local] ist Gerätezeit wie vom SDK. */
	@JvmStatic
	@JvmOverloads
	fun isPlaceholderTime(local: java.time.LocalDateTime, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Boolean {
		val utc = local.atZone(zone).withZoneSameInstant(java.time.ZoneOffset.UTC)
		return utc.hour == 0 && utc.minute == 0
	}
}
