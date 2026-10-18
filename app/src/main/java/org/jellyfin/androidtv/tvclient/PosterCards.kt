package org.jellyfin.androidtv.tvclient

import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

/**
 * Jellyfin-TV-Client: Serien, Staffeln und Folgen einheitlich als Hochformat-Poster (z. B. „Kürzlich
 * hinzugefügt“, wo der Server mal die Serie, mal eine einzelne Folge liefert).
 */
object PosterCards {
	private val types = setOf(BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE)

	@JvmStatic
	fun applies(row: BaseRowItem?): Boolean =
		row is BaseItemDtoBaseRowItem && row.uniformPoster && row.baseItem?.type in types

	/** Serie: ihr Poster. Staffel: ihr Poster, sonst das der Serie. Folge: das Poster der Serie. */
	@JvmStatic
	fun image(item: BaseItemDto?): JellyfinImage? {
		if (item == null) return null
		return when (item.type) {
			BaseItemKind.EPISODE -> item.seriesPrimaryImage
			else -> item.itemImages[ImageType.PRIMARY] ?: item.seriesPrimaryImage
		}
	}
}
