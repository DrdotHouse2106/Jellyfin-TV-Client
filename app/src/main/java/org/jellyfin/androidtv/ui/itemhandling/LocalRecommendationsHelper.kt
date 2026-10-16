package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.querying.ExternalItemsRequest
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import timber.log.Timber

/**
 * Lädt eine Zeile von einem Plugin-Endpunkt (z. B. "Local Recommendations" oder
 * "Home Screen Sections"). Ist das Plugin nicht installiert (oder zu alt) oder gibt es keine Einträge,
 * verschwindet die Zeile einfach.
 */
fun ItemRowAdapter.retrieveLocalRecommendations(api: ApiClient, query: ExternalItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.get<BaseItemDtoQueryResult>(
					pathTemplate = query.path,
					queryParameters = query.queryParameters,
				).content
			}
		}.fold(
			onSuccess = { response ->
				setItems(
					items = response.items,
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							!query.isLiveTv && preferParentThumb,
							isStaticHeight,
						)
					}
				)

				if (response.items.isEmpty()) removeRow()
				notifyRetrieveFinished()
			},
			onFailure = { error ->
				// Meist: Plugin fehlt oder ist älter als 12.5.0 (404) – dann keine Zeile anzeigen
				Timber.w(error, "Zeile %s konnte nicht geladen werden", query.path)
				removeRow()
				notifyRetrieveFinished()
			}
		)
	}
}
