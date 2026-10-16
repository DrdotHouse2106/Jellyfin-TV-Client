package org.jellyfin.androidtv.tvclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.androidtv.data.querying.GetHomeScreenSectionRequest
import org.jellyfin.androidtv.ui.home.HomeFragmentLocalRecommendationsRow
import org.jellyfin.androidtv.ui.home.HomeFragmentRow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import timber.log.Timber
import java.util.Locale
import java.util.UUID

/**
 * Jellyfin-TV-Client: Abschnitte des Plugins "Home Screen Sections" (wie in der Weboberfläche
 * eingerichtet, z. B. Genres, Top 10, „Weil du … gesehen hast“, Sammlungen) als Zeilen.
 * Abschnitte, die Quallenkino schon selbst zeigt, werden übersprungen.
 */
object HomeScreenSections {
	@Serializable
	data class Section(
		@SerialName("Section") val section: String? = null,
		@SerialName("DisplayText") val displayText: String? = null,
		@SerialName("AdditionalData") val additionalData: String? = null,
	)

	@Serializable
	data class SectionList(
		@SerialName("Items") val items: List<Section> = emptyList(),
	)

	/** Abschnitte, die Quallenkino selbst hat oder die keine abspielbaren Einträge liefern. */
	private val skipped = setOf(
		"ContinueWatching", "NextUp", "ContinueWatchingNextUp", "MyMedia", "LiveTV",
		"LocalRecsMovies", "LocalRecsShows", "LocalRecsLiveTv",
		"Discover", "DiscoverMovies", "DiscoverTV", "MyJellyseerrRequests",
	).map { it.lowercase() }.toSet()

	fun isShown(section: Section) =
		!section.section.isNullOrBlank() && section.section.lowercase() !in skipped

	/** Lädt die Abschnitte des Benutzers. Ohne Plugin (oder bei Fehlern) gibt es keine Zeilen. */
	suspend fun loadRows(api: ApiClient, userId: UUID): List<HomeFragmentRow> {
		val language = Locale.getDefault().language
		val sections = try {
			api.get<SectionList>(
				pathTemplate = "/HomeScreen/Sections",
				queryParameters = mapOf("userId" to userId.toString(), "language" to language),
			).content.items
		} catch (@Suppress("TooGenericExceptionCaught") err: Exception) {
			Timber.i(err, "Home Screen Sections nicht verfügbar")
			return emptyList()
		}

		return sections.filter(::isShown).map { section ->
			HomeFragmentLocalRecommendationsRow(
				title = section.displayText?.takeIf { it.isNotBlank() } ?: section.section.orEmpty(),
				query = GetHomeScreenSectionRequest(
					section = section.section.orEmpty(),
					userId = userId.toString(),
					additionalData = section.additionalData,
					language = language,
				),
			)
		}
	}
}
