package org.jellyfin.androidtv.tvclient

import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentBackdropImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType

/**
 * Jellyfin-TV-Client: Serien, Staffeln und Folgen einheitlich im Querformat (z. B. „Kürzlich
 * hinzugefügt“, wo der Server mal die Serie, mal eine einzelne Folge liefert).
 */
object UniformCards {
	private val types = setOf(BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE)

	@JvmStatic
	fun applies(row: BaseRowItem?): Boolean =
		row is BaseItemDtoBaseRowItem && row.uniformCards && row.baseItem?.type in types

	/** Vorschaubild der Serie, sonst ihr Hintergrund, bei Folgen zuletzt das Standbild der Folge. */
	@JvmStatic
	fun image(item: BaseItemDto?): JellyfinImage? {
		if (item == null) return null
		return when (item.type) {
			BaseItemKind.SERIES -> item.itemImages[ImageType.THUMB] ?: item.itemBackdropImages.firstOrNull()
			else -> item.parentImages[ImageType.THUMB]
				?: item.parentBackdropImages.firstOrNull()
				?: item.itemImages[ImageType.THUMB]
				?: item.itemBackdropImages.firstOrNull()
				?: item.itemImages[ImageType.PRIMARY].takeIf { item.type == BaseItemKind.EPISODE }
		}
	}
}
