package org.jellyfin.androidtv.tvclient

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.studiosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Jellyfin-TV-Client: „Welten“ wie Marvel, Disney oder Pixar. Eine Welt fasst Titel über die Studios
 * und passende Sammlungen (Filmreihen) zusammen, weil die Studio-Angaben oft lückenhaft sind.
 * Welten mit zu wenigen Titeln werden nicht gezeigt.
 */
object Worlds {
	const val MIN_ITEMS = 5
	private const val DEFAULT_COLOR = 0xFF8A8A8A
	private const val LATEST_CANDIDATES = 120
	private const val LOG_TAG = "Quallenkino"
	private const val CACHE_MS = 10 * 60 * 1000L
	private const val ROW_LIMIT = 40
	private const val TIMEOUT_MS = 8_000
	private val types = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
	private val json = Json { ignoreUnknownKeys = true }

	/**
	 * Feste Welten. [studios] und [collections] sind kleingeschriebene Wortteile, die im Studio- bzw.
	 * Sammlungsnamen vorkommen müssen. Die Reihenfolge der Sammlungen ist die Reihenfolge der Reihen.
	 */
	data class Definition(
		val key: String,
		val name: String,
		val color: Long,
		val studios: List<String>,
		val collections: List<String>,
		/** Weitere Namen bzw. Ids der Welt beim Plugin (z. B. nach einer Umbenennung). */
		val aliases: List<String> = emptyList(),
		/** Id der Welt beim Plugin, wenn die Liste von dort kommt. */
		val pluginId: String? = null,
	)

	val definitions = listOf(
		Definition(
			"marvel", "Marvel", 0xFFB3141B, listOf("marvel"),
			listOf("avengers", "iron man", "captain america", "thor", "guardians of the galaxy", "black panther", "doctor strange", "ant-man", "spider-man", "x-men", "deadpool", "wolverine", "hulk"),
		),
		Definition(
			"disney", "Disney", 0xFF113CCF, listOf("walt disney"),
			listOf("eiskönigin", "frozen", "könig der löwen", "lion king", "fluch der karibik", "pirates of the caribbean", "dschungelbuch", "vaiana", "moana", "aladdin", "arielle", "cinderella", "101 dalmatiner", "die schöne und das biest", "bambi"),
		),
		Definition(
			"pixar", "Pixar", 0xFF1C7ED6, listOf("pixar"),
			listOf("toy story", "findet nemo", "finding nemo", "die unglaublichen", "incredibles", "cars", "monster ag", "monsters", "alles steht kopf", "inside out"),
		),
		Definition(
			"illumination", "Minions & Co.", 0xFFF2C200, listOf("illumination"),
			listOf("minions", "ich - einfach unverbesserlich", "ich – einfach unverbesserlich", "despicable me", "pets", "sing", "super mario"),
		),
		Definition(
			"dreamworks", "DreamWorks", 0xFF2F6FB5, listOf("dreamworks"),
			listOf("shrek", "madagascar", "kung fu panda", "drachenzähmen", "how to train your dragon", "trolls", "croods", "boss baby", "gestiefelte kater", "puss in boots"),
		),
		Definition(
			"dc", "DC", 0xFF0476F2, listOf("dc comics", "dc entertainment", "dc films", "dc studios"),
			listOf("batman", "the dark knight", "superman", "justice league", "wonder woman", "aquaman", "suicide squad"),
		),
		Definition("starwars", "Star Wars", 0xFF2B2B2B, listOf("lucasfilm"), listOf("star wars")),
		Definition("ghibli", "Studio Ghibli", 0xFF4E8F3A, listOf("ghibli"), emptyList()),
		Definition(
			"jamesbond", "James Bond", 0xFF7A6A3A, listOf("eon productions"),
			listOf("james bond", "007"),
		),
		Definition(
			"jurassicpark", "Jurassic Park", 0xFF3E5C2E, listOf("amblin"),
			listOf("jurassic park", "jurassic world"),
		),
		Definition(
			"harrypotter", "Wizarding World", 0xFF6B4E16, listOf("heyday films"),
			listOf("harry potter", "phantastische tierwesen", "fantastic beasts"),
			aliases = listOf("Harry Potter", "wizarding-world"),
		),
		Definition(
			"mittelerde", "Mittelerde", 0xFF5A4A2A, listOf("wingnut"),
			listOf("herr der ringe", "lord of the rings", "der hobbit", "the hobbit"),
		),
	)

	data class World(
		val definition: Definition,
		val studioIds: List<UUID>,
		val collections: List<BaseItemDto>,
		val count: Int,
		/** Bild des Studios (Logo), falls der Server eines hat. */
		val imageUrl: String?,
		/** Hintergrundbild eines Titels der Welt (abgedunkelt hinter der Kachel). */
		val backdropUrl: String? = null,
		/** Titel mit lokalem Trailer für die Vorschau (Plugin-Feld „TrailerItemId“). */
		val trailerItemId: String? = null,
		/** Reihenfolge nach Handlungszeit (Plugin ab 12.55, Feld „Chronology“), sonst leer. */
		val chronology: List<ChronologyEntry> = emptyList(),
	) {
		val key get() = definition.key
		val name get() = definition.name
	}

	/** Abschnitt bzw. Titel mit Handlungszeit (bei Serien ggf. nur bestimmte Staffeln). */
	data class ChronologyEntry(val section: String? = null, val itemId: UUID? = null, val label: String? = null, val seasons: List<Int> = emptyList()) {
		/** „2256–2258 · Staffel 1–2“; ohne Zeitangabe nur die Staffeln. */
		val caption: String
			get() = listOfNotNull(label?.takeIf { it.isNotBlank() }, seasonsText(seasons)).joinToString(" · ")
	}

	/** [captions] (falls gesetzt) steht je Karte darunter, z. B. die Handlungszeit. */
	data class Section(val title: String, val items: List<BaseItemDto>, val captions: List<String?>? = null)

	/** Überschriften der Welt-Seite (aus den Ressourcen). */
	data class Labels(val forYou: String, val latest: String, val movies: String, val series: String)

	@Volatile
	private var cache: Triple<String, Long, List<World>>? = null

	/** Passt ein Name zu einem der Wortteile? */
	fun matches(name: String?, parts: List<String>): Boolean {
		val lower = name?.lowercase() ?: return false
		return parts.any { lower.contains(it) }
	}

	/** Position des ersten passenden Wortteils (für die Reihenfolge der Sammlungen). */
	fun collectionRank(name: String?, parts: List<String>): Int {
		val lower = name?.lowercase() ?: return Int.MAX_VALUE
		return parts.indexOfFirst { lower.contains(it) }.let { if (it < 0) Int.MAX_VALUE else it }
	}

	/** „Iron Man Filmreihe“ → „Iron Man“. */
	fun collectionTitle(name: String): String = name
		.replace(Regex("\\s*[-–]?\\s*(Filmreihe|Collection|Sammlung|Reihe)$", RegexOption.IGNORE_CASE), "")
		.trim()
		.ifEmpty { name }

	/** Welten mit genug Titeln, für die Startseite. */
	suspend fun available(api: ApiClient, userId: UUID): List<World> = withContext(Dispatchers.IO) {
		val cacheKey = "${api.baseUrl}|$userId"
		cache?.takeIf { it.first == cacheKey && System.currentTimeMillis() - it.second < CACHE_MS }?.let { return@withContext it.third }

		val worlds = runCatching {
			val studios = api.studiosApi.getStudios(userId = userId, includeItemTypes = types, enableTotalRecordCount = false).content.items
			val boxSets = api.itemsApi.getItems(
				GetItemsRequest(
					userId = userId,
					includeItemTypes = setOf(BaseItemKind.BOX_SET),
					recursive = true,
					fields = setOf(ItemFields.CHILD_COUNT),
					// Nur Name und Anzahl werden gebraucht – ohne Bilder und Benutzerdaten antwortet der Server schneller
					enableImages = false,
					enableUserData = false,
					enableTotalRecordCount = false,
				)
			).content.items

			// Welten des Plugins (Liste, Reihenfolge, Logos, Bilder) – ohne Plugin die eigene Liste
			val pluginWorlds = pluginGet(api, "/LocalRecs/Worlds")
			val fromPlugin = pluginWorlds?.let(::pluginDefinitions).orEmpty()
			val chosen = fromPlugin.ifEmpty { definitions.map { it to null } }

			coroutineScope {
				chosen.map { (definition, pluginCount) ->
					async {
						val matchedStudios = studios.filter { matches(it.name, definition.studios) }
						val matchedCollections = boxSets
							.filter { matches(it.name, definition.collections) }
							.sortedBy { collectionRank(it.name, definition.collections) }
						val studioCount = if (matchedStudios.isEmpty()) 0 else runCatching {
							api.itemsApi.getItems(
								GetItemsRequest(
									userId = userId,
									studioIds = matchedStudios.map { it.id },
									includeItemTypes = types,
									recursive = true,
									limit = 1,
									enableTotalRecordCount = true,
								)
							).content.totalRecordCount
						}.getOrDefault(0)
						// Das Plugin kennt die genaue Zahl (nur für den Benutzer sichtbare Titel)
						val count = pluginCount ?: (studioCount + matchedCollections.sumOf { it.childCount ?: 0 })
						val image = matchedStudios.firstNotNullOfOrNull { studio ->
							(studio.itemImages[ImageType.THUMB] ?: studio.itemImages[ImageType.PRIMARY])?.getUrl(api, maxWidth = 600)
						}
						val pluginImage = pluginWorlds?.let { pluginImageItemId(it, definition) }
						val backdrop = pluginImage?.let { "${api.baseUrl?.trimEnd('/')}/Items/$it/Images/Backdrop?maxWidth=640&quality=80" }
							?: matchedCollections.firstNotNullOfOrNull { heroBackdropImage(it)?.getUrl(api, maxWidth = 640) }
						// Logo der Welt vom eigenen Server (Plugin, aus dem Welten-Repo), sonst das Studio-Bild
						val logo = pluginWorlds?.let { pluginField(it, definition, "LogoUrl") }?.let { serverUrl(api, it) }
						val trailerItemId = pluginWorlds?.let { pluginField(it, definition, "TrailerItemId") }
						val chronology = pluginWorlds?.let { pluginChronology(it, definition) }.orEmpty()
						World(definition, matchedStudios.map { it.id }, matchedCollections, count, logo ?: image, backdrop, trailerItemId, chronology)
					}
				}.awaitAll()
			}.filter { it.count >= MIN_ITEMS }
		}.onFailure { Timber.w(it, "Welten konnten nicht geladen werden") }.getOrDefault(emptyList())

		cache = Triple(cacheKey, System.currentTimeMillis(), worlds)
		worlds
	}

	suspend fun find(api: ApiClient, userId: UUID, key: String): World? = available(api, userId).firstOrNull { it.key == key }

	/** Reihen der Welt-Seite. */
	suspend fun sections(api: ApiClient, userId: UUID, world: World, labels: Labels): List<Section> = withContext(Dispatchers.IO) {
		suspend fun query(request: GetItemsRequest) = runCatching { api.itemsApi.getItems(request).content.items }
			.onFailure { Timber.w(it, "Reihe der Welt konnte nicht geladen werden") }
			.getOrDefault(emptyList())

		fun byStudio(types: Set<BaseItemKind>, sortBy: ItemSortBy, order: SortOrder) = GetItemsRequest(
			userId = userId,
			studioIds = world.studioIds,
			includeItemTypes = types,
			recursive = true,
			sortBy = setOf(sortBy),
			sortOrder = setOf(order),
			limit = ROW_LIMIT,
			fields = ItemRepository.browseFields,
			enableTotalRecordCount = false,
		)

		coroutineScope {
			val forYou = async { forYou(api, userId, world) }
			val collections = world.collections.map { collection ->
				async {
					Section(
						collectionTitle(collection.name.orEmpty()),
						query(
							GetItemsRequest(
								userId = userId,
								parentId = collection.id,
								sortBy = setOf(ItemSortBy.PREMIERE_DATE),
								sortOrder = setOf(SortOrder.ASCENDING),
								fields = ItemRepository.browseFields,
								enableTotalRecordCount = false,
							)
						),
					)
				}
			}
			val pluginId = world.definition.pluginId
			val hasStudios = world.studioIds.isNotEmpty()
			// Mit Plugin: alle Titel der Welt genau wie dort zugeordnet (auch über TMDB-Ids, mit Ausschlüssen)
			val movies = async {
				pluginId?.let { pluginItems(api, userId, it, "type=movies&sort=year&limit=$ROW_LIMIT") }
					?: if (hasStudios) query(byStudio(setOf(BaseItemKind.MOVIE), ItemSortBy.PREMIERE_DATE, SortOrder.DESCENDING)) else emptyList()
			}
			val series = async {
				pluginId?.let { pluginItems(api, userId, it, "type=series&sort=year&limit=$ROW_LIMIT") }
					?: if (hasStudios) query(byStudio(setOf(BaseItemKind.SERIES), ItemSortBy.PREMIERE_DATE, SortOrder.DESCENDING)) else emptyList()
			}
			val latest = async {
				if (pluginId != null) {
					// Zuletzt auf dem Server hinzugekommen: ab Plugin 12.15.1 direkt (sort=added), sonst über die Ids
					pluginItems(api, userId, pluginId, "sort=added&limit=20")?.takeIf { it.isNotEmpty() }?.let { return@async it }
					val ids = (movies.await() + series.await()).map { it.id }.take(LATEST_CANDIDATES)
					if (ids.isEmpty()) emptyList() else query(
						GetItemsRequest(
							userId = userId,
							ids = ids,
							sortBy = setOf(ItemSortBy.DATE_CREATED),
							sortOrder = setOf(SortOrder.DESCENDING),
							limit = 20,
							fields = ItemRepository.browseFields,
							enableTotalRecordCount = false,
						)
					)
				} else if (hasStudios) query(byStudio(types, ItemSortBy.DATE_CREATED, SortOrder.DESCENDING)) else emptyList()
			}

			listOf(
				Section(labels.forYou, forYou.await()),
				Section(labels.latest, latest.await().take(20)),
			) + collections.awaitAll() + listOf(
				Section(labels.movies, movies.await()),
				Section(labels.series, series.await()),
			)
		}.filter { it.items.isNotEmpty() }
	}

	/**
	 * Optional: „Für dich in dieser Welt“ vom Plugin Local Recommendations (ab 12.10): Die Welt des
	 * Plugins wird über den Namen gefunden (GET /LocalRecs/Worlds), die Titel kommen von
	 * GET /LocalRecs/Worlds/{id}/Recommendations. Ohne Plugin oder ohne passende Welt bleibt die Reihe weg.
	 */
	private suspend fun forYou(api: ApiClient, userId: UUID, world: World): List<BaseItemDto> {
		val worldsText = pluginGet(api, "/LocalRecs/Worlds") ?: return emptyList()
		val pluginId = pluginWorldId(worldsText, world.definition) ?: return emptyList()
		val ids = pluginGet(api, "/LocalRecs/Worlds/$pluginId/Recommendations?limit=20")?.let(::parseIds).orEmpty()
		if (ids.isEmpty()) return emptyList()

		val items = runCatching {
			api.itemsApi.getItems(GetItemsRequest(userId = userId, ids = ids, fields = ItemRepository.browseFields)).content.items
		}.getOrDefault(emptyList()).associateBy { it.id }
		return ids.mapNotNull { items[it] }
	}

	/**
	 * Titel einer Plugin-Welt in der Reihenfolge des Plugins; null, wenn das Plugin den Endpunkt nicht hat.
	 * Vom Plugin kommen nur die Ids, die Titel selbst lädt Jellyfin (mit allen Feldern für die Karten).
	 */
	private suspend fun pluginItems(api: ApiClient, userId: UUID, pluginId: String, query: String): List<BaseItemDto>? {
		val text = pluginGet(api, "/LocalRecs/Worlds/$pluginId/Items?$query") ?: return null
		val ids = parseIds(text)
		if (ids.isEmpty()) return emptyList()
		val byId = runCatching {
			api.itemsApi.getItems(GetItemsRequest(userId = userId, ids = ids, fields = ItemRepository.browseFields, enableTotalRecordCount = false)).content.items
		}.onFailure { Log.w(LOG_TAG, "Welt $pluginId: Titel nicht geladen", it) }.getOrDefault(emptyList()).associateBy { it.id }
		Log.i(LOG_TAG, "Welt $pluginId ($query): ${ids.size} Ids vom Plugin, ${byId.size} Titel geladen")
		return ids.mapNotNull { byId[it] }
	}

	/** {"Items":[BaseItemDto …]} lesen; fehlerhafte Einträge werden übersprungen. */
	fun parseItems(text: String): List<BaseItemDto> {
		val items = (runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject)?.get("Items") as? JsonArray ?: return emptyList()
		return items.mapNotNull { element ->
			runCatching { json.decodeFromJsonElement(BaseItemDto.serializer(), element) }.getOrNull()
		}
	}

	/** Kurz gemerkte Antworten des Plugins (Startseite und „Für dich“ fragen dieselbe Welten-Liste ab). */
	private val pluginCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, String>>()

	private fun pluginGet(api: ApiClient, path: String): String? {
		val base = api.baseUrl?.trimEnd('/') ?: return null
		val token = api.accessToken ?: return null
		val key = "$base|${token.hashCode()}|$path"
		pluginCache[key]?.takeIf { System.currentTimeMillis() - it.first < CACHE_MS }?.let { return it.second }
		return pluginGetUncached(base, token, path)?.also { pluginCache[key] = System.currentTimeMillis() to it }
	}

	private fun pluginGetUncached(base: String, token: String, path: String): String? {
		return runCatching {
			val connection = URL(base + path).openConnection() as HttpURLConnection
			try {
				connection.connectTimeout = TIMEOUT_MS
				connection.readTimeout = TIMEOUT_MS
				connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
				connection.setRequestProperty("Accept", "application/json")
				if (connection.responseCode == 200) connection.inputStream.bufferedReader().use { it.readText() } else null
			} finally {
				connection.disconnect()
			}
		}.getOrNull()
	}

	/** Kleingeschrieben, nur Buchstaben und Ziffern, Umlaute ausgeschrieben („Star Wars“ → „starwars“). */
	fun normalize(text: String): String = text.lowercase()
		.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
		.filter { it.isLetterOrDigit() }

	/**
	 * Welten aus der Liste des Plugins, mit der eigenen Definition verknüpft (für Akzentfarbe und
	 * Schlüssel), soweit eine passt. Studios und Sammlungen kommen vom Plugin.
	 */
	fun pluginDefinitions(text: String): List<Pair<Definition, Int?>> {
		val list = runCatching { json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return emptyList()
		fun JsonObject.strings(key: String) = (this[key] as? JsonArray).orEmpty()
			.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.trim()?.lowercase()?.takeIf { s -> s.isNotEmpty() } }

		return list.mapNotNull { element ->
			val obj = element as? JsonObject ?: return@mapNotNull null
			val id = (obj["Id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
			val name = (obj["Name"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: id
			val local = definitions.firstOrNull { pluginWorldId(JsonArray(listOf(obj)).toString(), it) == id }
			val definition = Definition(
				key = local?.key ?: normalize(id),
				name = name,
				color = local?.color ?: DEFAULT_COLOR,
				studios = obj.strings("Studios").ifEmpty { local?.studios.orEmpty() },
				collections = obj.strings("Collections").ifEmpty { local?.collections.orEmpty() },
				aliases = listOf(id) + local?.aliases.orEmpty(),
				pluginId = id,
			)
			definition to (obj["ItemCount"] as? JsonPrimitive)?.content?.toIntOrNull()
		}
	}

	/** ImageItemId der passenden Plugin-Welt. */
	fun pluginImageItemId(text: String, definition: Definition): String? = pluginField(text, definition, "ImageItemId")

	/** „Chronology“ der passenden Plugin-Welt (testbar); null oder fehlend → leer. */
	fun pluginChronology(text: String, definition: Definition): List<ChronologyEntry> {
		val id = pluginWorldId(text, definition) ?: return emptyList()
		val list = runCatching { json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return emptyList()
		val world = list.mapNotNull { it as? JsonObject }.firstOrNull { (it["Id"] as? JsonPrimitive)?.content == id } ?: return emptyList()
		return (world["Chronology"] as? JsonArray).orEmpty().mapNotNull { entry ->
			val obj = entry as? JsonObject ?: return@mapNotNull null
			fun text(key: String) = (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
			text("Section")?.let { return@mapNotNull ChronologyEntry(section = it) }
			val itemId = text("ItemId")?.toUUIDOrNull() ?: return@mapNotNull null
			val seasons = (obj["Seasons"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }
			ChronologyEntry(itemId = itemId, label = text("Label"), seasons = seasons)
		}
	}

	/** „Staffel 1–2“, „Staffel 3“, „Staffel 1, 3“ (testbar); leer → null. */
	fun seasonsText(seasons: List<Int>): String? {
		if (seasons.isEmpty()) return null
		val sorted = seasons.sorted()
		if (sorted.size == 1) return "Staffel ${sorted[0]}"
		val consecutive = sorted.zipWithNext().all { (a, b) -> b == a + 1 }
		return if (consecutive) "Staffel ${sorted.first()}–${sorted.last()}" else "Staffel ${sorted.joinToString(", ")}"
	}

	/** Chronologie in Abschnitte teilen (testbar): jede Überschrift beginnt eine neue Reihe, leere fallen weg. */
	fun chronologyGroups(entries: List<ChronologyEntry>, fallbackTitle: String): List<Pair<String, List<ChronologyEntry>>> {
		val groups = mutableListOf<Pair<String, MutableList<ChronologyEntry>>>()
		for (entry in entries) {
			when {
				entry.section != null -> groups += entry.section to mutableListOf()
				entry.itemId != null -> {
					if (groups.isEmpty()) groups += fallbackTitle to mutableListOf()
					groups.last().second += entry
				}
			}
		}
		return groups.filter { it.second.isNotEmpty() }
	}

	/** Welt-Seite „Chronologisch (Handlungszeit)“: Abschnitte in Handlungsreihenfolge, Zeitangabe unter jeder Karte. */
	suspend fun chronologySections(api: ApiClient, userId: UUID, world: World, fallbackTitle: String): List<Section> = withContext(Dispatchers.IO) {
		val groups = chronologyGroups(world.chronology, fallbackTitle)
		val ids = groups.flatMap { (_, entries) -> entries.mapNotNull { it.itemId } }.distinct()
		val byId = runCatching {
			api.itemsApi.getItems(GetItemsRequest(userId = userId, ids = ids, fields = ItemRepository.browseFields, enableTotalRecordCount = false)).content.items
		}.onFailure { Timber.w(it, "Chronologie der Welt konnte nicht geladen werden") }.getOrDefault(emptyList()).associateBy { it.id }
		groups.mapNotNull { (title, entries) ->
			val found = entries.mapNotNull { entry -> entry.itemId?.let(byId::get)?.let { it to entry.caption.ifBlank { null } } }
			if (found.isEmpty()) null else Section(title, found.map { it.first }, found.map { it.second })
		}
	}

	/** Ein Textfeld der passenden Plugin-Welt (z. B. „LogoUrl“). */
	fun pluginField(text: String, definition: Definition, field: String): String? {
		val id = pluginWorldId(text, definition) ?: return null
		val list = runCatching { json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return null
		return list.mapNotNull { it as? JsonObject }
			.firstOrNull { (it["Id"] as? JsonPrimitive)?.content == id }
			?.let { it[field] as? JsonPrimitive }
			?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
	}

	/** Adresse auf dem eigenen Server („/LocalRecs/…“) vollständig machen; die Anmeldung kommt per Header (ServerAuth). */
	fun serverUrl(api: ApiClient, url: String): String? {
		if (url.startsWith("http://") || url.startsWith("https://")) return url
		val base = api.baseUrl?.trimEnd('/') ?: return null
		val path = if (url.startsWith("/")) url else "/$url"
		return base + path
	}

	/** Id der Plugin-Welt, die zu dieser Welt passt (über Id, Name oder Studio), sonst null. */
	fun pluginWorldId(text: String, definition: Definition): String? {
		val list = runCatching { json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return null
		val candidates = (listOf(definition.name, definition.key) + definition.aliases).map(::normalize).toSet()
		return list.firstNotNullOfOrNull { element ->
			val obj = element as? JsonObject ?: return@firstNotNullOfOrNull null
			val id = (obj["Id"] as? JsonPrimitive)?.content ?: return@firstNotNullOfOrNull null
			val name = (obj["Name"] as? JsonPrimitive)?.content.orEmpty()
			val keys = setOf(normalize(id), normalize(name))
			val matched = keys.any { it in candidates } ||
				candidates.any { candidate -> keys.any { it.contains(candidate) } } ||
				definition.studios.any { name.lowercase().contains(it) }
			id.takeIf { matched }
		}
	}

	fun parseIds(text: String): List<UUID> {
		val root = json.parseToJsonElement(text)
		val list: List<JsonElement> = when (root) {
			is JsonArray -> root
			is JsonObject -> (root["Items"] ?: root["items"]) as? JsonArray ?: emptyList()
			else -> emptyList()
		}
		return list.mapNotNull { element ->
			val raw = when (element) {
				is JsonPrimitive -> element.content
				is JsonObject -> (element["Id"] as? JsonPrimitive)?.content
				else -> null
			}
			raw?.toUUIDOrNull()
		}.distinct()
	}
}
