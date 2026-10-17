package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.leanback.widget.ListRowPresenter
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.tvclient.HomeBillboard
import org.jellyfin.androidtv.tvclient.HomeBillboardButtons
import org.jellyfin.androidtv.tvclient.HomeHero
import org.jellyfin.androidtv.tvclient.HomeHeroState
import org.jellyfin.androidtv.tvclient.HomeRow
import org.jellyfin.androidtv.tvclient.SideRail
import org.jellyfin.androidtv.tvclient.HomeHeroBackdrop
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto

private const val BILLBOARD_FOCUS_GUARD_MS = 8_000L

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val userPreferences by inject<UserPreferences>()

	// Jellyfin-TV-Client: Banner schon einmal gestartet? Dann beim Zurückkehren keinen Fokus mehr erzwingen.
	private var billboardStarted = false

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val rowsFocusRequester = remember { FocusRequester() }
		val showHero = remember { userPreferences[HomeRow.heroPreference] }
		LaunchedEffect(rowsFocusRequester) { rowsFocusRequester.requestFocus() }

		// Jellyfin-TV-Client: Billboard mit wechselnden Vorschlägen (Netflix-Ansicht)
		val api = koinInject<ApiClient>()
		val billboardFocusRequester = remember { FocusRequester() }
		val railHomeFocusRequester = remember { FocusRequester() }
		var billboardItems by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
		var billboardFocused by remember { mutableStateOf(false) }
		// Hat der Benutzer schon eine Taste gedrückt? Dann wird ihm der Fokus nicht mehr weggenommen.
		var userInteracted by remember { mutableStateOf(false) }
		if (showHero) LaunchedEffect(Unit) {
			// Zurück von einer anderen Seite: Fokus und Titel bleiben, wo sie waren
			val firstStart = !billboardStarted
			billboardStarted = true
			if (firstStart) HomeHeroState.rowsFocusBlocked.value = true
			billboardItems = HomeBillboard.load(api)
			if (billboardItems.isEmpty() || !firstStart) HomeHeroState.rowsFocusBlocked.value = false
			if (firstStart && billboardItems.isNotEmpty()) {
				HomeHeroState.item.value = billboardItems.first()
				// Beim Öffnen (z. B. nach dem Anmelden) auf „Abspielen“ im Banner starten. Die Zeilen laden
				// danach noch und holen sich dabei sonst den Fokus – deshalb einige Sekunden lang den Fokus
				// zurück ins Banner holen, solange niemand eine Taste gedrückt hat.
				val until = System.currentTimeMillis() + BILLBOARD_FOCUS_GUARD_MS
				while (!userInteracted && System.currentTimeMillis() < until) {
					if (!billboardFocused) runCatching { billboardFocusRequester.requestFocus() }
					delay(100)
				}
			}
		}

		JellyfinTheme {
			Box(
				Modifier
					.fillMaxSize()
					.onPreviewKeyEvent {
						// Erste Taste: Zeilen wieder freigeben (vor der Fokussuche, damit „Runter“ klappt)
						userInteracted = true
						HomeHeroState.rowsFocusBlocked.value = false
						false
					}
			) {
				// Jellyfin-TV-Client: Netflix-Ansicht – Bild oben rechts, Navigation links, Infobereich oben
				if (showHero) HomeHeroBackdrop()

				Row(Modifier.fillMaxSize()) {
					// „Rechts“ aus der Navigation führt zurück ins Banner (wenn es da ist), sonst in die Zeilen
					if (showHero) SideRail(
						contentFocusRequester = if (billboardItems.isNotEmpty()) billboardFocusRequester else rowsFocusRequester,
						homeFocusRequester = railHomeFocusRequester,
					)

					Column(Modifier.weight(1f)) {
						if (showHero) {
							HomeHero(Modifier.fillMaxWidth().height(200.dp))
							HomeBillboardButtons(
								items = billboardItems,
								modifier = Modifier
									.padding(start = 28.dp, top = 4.dp, bottom = 12.dp)
									.onFocusChanged {
										billboardFocused = it.hasFocus
										HomeHeroState.billboardFocused.value = it.hasFocus
									},
								focusRequester = billboardFocusRequester,
								downFocusRequester = rowsFocusRequester,
								leftFocusRequester = railHomeFocusRequester,
							)
						} else {
							MainToolbar(MainToolbarActiveButton.Home)
						}

						// The leanback code has its own awful focus handling that doesn't work properly with Compose view inteop to workaround this
						// issue we add custom behavior that only allows focus exit when the current selected row is the first one. Additionally when
						// we do switch the focus, we reset the leanback state so it won't cause weird behavior when focus is regained
						var rowsSupportFragment by remember { mutableStateOf<HomeRowsFragment?>(null) }
						AndroidFragment<HomeRowsFragment>(
							modifier = Modifier
								.focusGroup()
								.focusRequester(rowsFocusRequester)
								.focusProperties {
									onExit = {
										val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
										when {
											// Netflix-Ansicht: mit „Links“ in die Navigation am Rand – nur vom ersten Titel einer Zeile.
											// Der Fokus der Zeilen wird dabei gelöscht, sonst öffnet OK weiterhin den zuletzt gewählten Titel.
											showHero && requestedFocusDirection == FocusDirection.Left -> {
												val fragment = rowsSupportFragment
												val rowHolder = fragment?.getRowViewHolder(fragment.selectedPosition) as? ListRowPresenter.ViewHolder
												val column = rowHolder?.gridView?.selectedPosition ?: 0
												if (column > 0) cancelFocusChange()
												else fragment?.verticalGridView?.clearFocus()
											}
											// Netflix-Ansicht: aus der ersten Zeile mit „Hoch“ zu den Billboard-Knöpfen
											requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected -> cancelFocusChange()
											else -> {
												rowsSupportFragment?.selectedPosition = 0
												rowsSupportFragment?.verticalGridView?.clearFocus()
											}
										}
									}
								}
								.fillMaxSize(),
							onUpdate = { fragment ->
								rowsSupportFragment = fragment
							}
						)
					}
				}

				if (showHero) {
					ToolbarClock(
						Modifier
							.align(Alignment.TopEnd)
							.padding(top = 24.dp, end = 48.dp)
					)
				}
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}
}
