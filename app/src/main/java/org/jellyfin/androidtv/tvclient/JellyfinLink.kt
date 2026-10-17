package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID

/** Jellyfin-TV-Client: Verbindung zwischen Jellyseerr-Treffern und Jellyfin bzw. YouTube. */
object JellyfinLink {
	private val youtubePackages = listOf(
		"com.google.android.youtube.tv",
		"com.amazon.firetv.youtube",
		"com.google.android.youtube.googletv",
		"com.google.android.youtube",
	)

	/**
	 * Den Titel in Jellyfin finden. Die von Jellyseerr gemerkte ID kann veraltet sein (z. B. nach dem
	 * Umbenennen eines Ordners), deshalb notfalls per Titelsuche und TMDB-ID (AnyProviderIdEquals
	 * wird von manchen Servern ignoriert).
	 */
	suspend fun resolve(api: ApiClient, media: Jellyseerr.Media, userId: UUID?): BaseItemDto? = withContext(Dispatchers.IO) {
		media.jellyfinId?.let { id ->
			runCatching { api.userLibraryApi.getItem(id).content }.getOrNull()?.let { return@withContext it }
		}

		val type = if (media.type == "tv") BaseItemKind.SERIES else BaseItemKind.MOVIE
		runCatching {
			api.itemsApi.getItems(
				GetItemsRequest(
					userId = userId,
					searchTerm = media.title,
					includeItemTypes = setOf(type),
					recursive = true,
					fields = setOf(ItemFields.PROVIDER_IDS),
					limit = 50,
					enableTotalRecordCount = false,
				)
			).content.items.firstOrNull { item ->
				item.providerIds?.entries?.any { it.key.equals("Tmdb", true) && it.value == media.id.toString() } == true
			}
		}.onFailure { Timber.w(it, "Titel in Jellyfin nicht gefunden") }.getOrNull()
	}

	/** YouTube-Trailer direkt in der YouTube-App öffnen (statt Auswahl „Öffnen mit …“). */
	fun youtubeIntent(context: Context, key: String): Intent {
		val uri = Uri.parse("https://www.youtube.com/watch?v=$key")
		val installed = youtubePackages.firstOrNull { context.packageManager.getLaunchIntentForPackage(it) != null ||
			runCatching { context.packageManager.getPackageInfo(it, 0) }.isSuccess }
		return Intent(Intent.ACTION_VIEW, uri).apply {
			if (installed != null) setPackage(installed)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
	}
}
