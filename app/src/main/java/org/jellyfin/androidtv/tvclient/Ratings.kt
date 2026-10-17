package org.jellyfin.androidtv.tvclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

/**
 * Jellyfin-TV-Client: Daumen hoch/runter. Folgen werden über ihre Serie bewertet, damit die Bewertung
 * in die Empfehlungen („Local Recommendations“) einfließt.
 */
object Ratings {
	/** Was bewertet wird: bei Folgen die Serie, sonst der Titel selbst. */
	fun target(item: BaseItemDto): UUID? = when (item.type) {
		BaseItemKind.EPISODE, BaseItemKind.SEASON -> item.seriesId
		BaseItemKind.MOVIE, BaseItemKind.SERIES -> item.id
		else -> null
	}

	/** Aktuelle Bewertung: true = gefällt mir, false = nichts für mich, null = keine. */
	suspend fun get(api: ApiClient, id: UUID): Boolean? = withContext(Dispatchers.IO) {
		runCatching { api.userLibraryApi.getItem(id).content.userData?.likes }.getOrNull()
	}

	/** Bewertung setzen oder mit null entfernen; gibt die neue Bewertung zurück. */
	suspend fun set(api: ApiClient, id: UUID, likes: Boolean?): Boolean? = withContext(Dispatchers.IO) {
		if (likes == null) api.userLibraryApi.deleteUserItemRating(id).content.likes
		else api.userLibraryApi.updateUserItemRating(id, likes = likes).content.likes
	}
}
