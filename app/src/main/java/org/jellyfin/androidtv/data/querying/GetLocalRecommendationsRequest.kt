package org.jellyfin.androidtv.data.querying

/**
 * Jellyfin-TV-Client: Abfrage einer Zeile über einen Server-Endpunkt, der eine Liste von
 * Elementen (QueryResult<BaseItemDto>) liefert.
 */
interface ExternalItemsRequest {
	val path: String
	val queryParameters: Map<String, Any?>

	/** Live-TV-Sendungen: kein Serien-Vorschaubild verwenden. */
	val isLiveTv: Boolean get() = false
}

/**
 * Abfrage der Empfehlungen des Plugins "Local Recommendations"
 * (GET /LocalRecs/Recommendations, ab Plugin-Version 12.5.0).
 */
data class GetLocalRecommendationsRequest(
	val type: LocalRecommendationsType,
	val limit: Int? = null,
) : ExternalItemsRequest {
	override val path get() = "/LocalRecs/Recommendations"
	override val queryParameters get() = mapOf("type" to type.serializedName, "limit" to limit)
	override val isLiveTv get() = type == LocalRecommendationsType.LIVE_TV
}

/**
 * Inhalt eines Abschnitts des Plugins "Home Screen Sections" (GET /HomeScreen/Section/{section}).
 */
data class GetHomeScreenSectionRequest(
	val section: String,
	val userId: String,
	val additionalData: String? = null,
	val language: String? = null,
) : ExternalItemsRequest {
	override val path get() = "/HomeScreen/Section/$section"
	override val queryParameters get() = mapOf("userId" to userId, "additionalData" to additionalData, "language" to language)
	override val isLiveTv get() = section.equals("LiveTV", ignoreCase = true)
}

enum class LocalRecommendationsType(val serializedName: String) {
	MOVIES("movies"),
	SERIES("series"),
	LIVE_TV("livetv"),
}
