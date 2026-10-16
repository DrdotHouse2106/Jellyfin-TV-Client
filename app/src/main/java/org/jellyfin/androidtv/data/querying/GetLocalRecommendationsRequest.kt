package org.jellyfin.androidtv.data.querying

/**
 * Abfrage der Empfehlungen des Plugins "Local Recommendations"
 * (GET /LocalRecs/Recommendations, ab Plugin-Version 12.5.0).
 */
data class GetLocalRecommendationsRequest(
	val type: LocalRecommendationsType,
	val limit: Int? = null,
)

enum class LocalRecommendationsType(val serializedName: String) {
	MOVIES("movies"),
	SERIES("series"),
	LIVE_TV("livetv"),
}
