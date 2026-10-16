package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.querying.GetLocalRecommendationsRequest
import org.jellyfin.androidtv.data.querying.LocalRecommendationsType
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import timber.log.Timber

/**
 * Lädt die Empfehlungen des Plugins "Local Recommendations" in eine Zeile.
 * Ist das Plugin nicht installiert (oder zu alt) oder gibt es keine Empfehlungen,
 * verschwindet die Zeile einfach.
 */
fun ItemRowAdapter.retrieveLocalRecommendations(api: ApiClient, query: GetLocalRecommendationsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.get<BaseItemDtoQueryResult>(
					pathTemplate = "/LocalRecs/Recommendations",
					queryParameters = mapOf(
						"type" to query.type.serializedName,
						"limit" to query.limit,
					),
				).content
			}
		}.fold(
			onSuccess = { response ->
				setItems(
					items = response.items,
					transform = { item, _ ->
						BaseItemDtoBaseRowItem(
							item,
							query.type != LocalRecommendationsType.LIVE_TV && preferParentThumb,
							isStaticHeight,
						)
					}
				)

				if (response.items.isEmpty()) removeRow()
				notifyRetrieveFinished()
			},
			onFailure = { error ->
				// Meist: Plugin fehlt oder ist älter als 12.5.0 (404) – dann keine Zeile anzeigen
				Timber.w(error, "Empfehlungen (%s) konnten nicht geladen werden", query.type.serializedName)
				removeRow()
				notifyRetrieveFinished()
			}
		)
	}
}
