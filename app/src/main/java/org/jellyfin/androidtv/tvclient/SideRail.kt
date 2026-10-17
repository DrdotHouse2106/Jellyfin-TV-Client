package org.jellyfin.androidtv.tvclient

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.koin.compose.koinInject

/**
 * Jellyfin-TV-Client: Navigation am linken Rand (wie bei Netflix) statt der Leiste oben.
 * Mit „Links“ aus der ersten Karte einer Zeile kommt man hinein.
 */
@Composable
fun SideRail(
	modifier: Modifier = Modifier,
	contentFocusRequester: FocusRequester? = null,
	homeFocusRequester: FocusRequester? = null,
) {
	val navigationRepository = koinInject<NavigationRepository>()
	val sessionRepository = koinInject<SessionRepository>()
	val mediaManager = koinInject<MediaManager>()
	val activity = LocalActivity.current

	// Feste Reihenfolge für hoch/runter – die automatische Fokussuche springt sonst in die Zeilen daneben
	val user = remember { FocusRequester() }
	val search = remember { FocusRequester() }
	val home = homeFocusRequester ?: remember { FocusRequester() }
	val guide = remember { FocusRequester() }
	val settings = remember { FocusRequester() }
	val requests = remember { FocusRequester() }
	val showRequests = remember { Jellyseerr.isConfigured() }
	val order = listOfNotNull(user, search, home, guide, requests.takeIf { showRequests }, settings)

	fun Modifier.railFocus(self: FocusRequester): Modifier {
		val index = order.indexOf(self)
		return this
			.focusRequester(self)
			.focusProperties {
				up = order.getOrNull(index - 1) ?: FocusRequester.Cancel
				down = order.getOrNull(index + 1) ?: FocusRequester.Cancel
				left = FocusRequester.Cancel
				right = contentFocusRequester ?: FocusRequester.Default
			}
	}

	Column(
		modifier = modifier
			.fillMaxHeight()
			.width(76.dp)
			.background(Brush.horizontalGradient(0f to Color.Black, 1f to Color.Black.copy(alpha = 0.6f)))
			.padding(vertical = 24.dp)
			.focusRestorer(home)
			.focusGroup(),
		verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		RailButton(R.drawable.ic_user, R.string.lbl_switch_user, Modifier.railFocus(user)) {
			mediaManager.clearAudioQueue()
			sessionRepository.destroyCurrentSession()
			activity?.startActivity(ActivityDestinations.startup(activity))
			activity?.finishAfterTransition()
		}
		RailButton(R.drawable.ic_search, R.string.lbl_search, Modifier.railFocus(search)) {
			navigationRepository.navigate(Destinations.search())
		}
		RailButton(R.drawable.ic_house, R.string.lbl_home, Modifier.railFocus(home), active = true) {}
		RailButton(R.drawable.ic_tv_guide, R.string.lbl_live_tv_guide, Modifier.railFocus(guide)) {
			navigationRepository.navigate(Destinations.liveTvGuide)
		}
		if (showRequests) {
			RailButton(R.drawable.ic_add, R.string.tvclient_jellyseerr_title, Modifier.railFocus(requests)) {
				navigationRepository.navigate(Destinations.jellyseerr)
			}
		}
		RailButton(R.drawable.ic_settings, R.string.lbl_settings, Modifier.railFocus(settings)) {
			activity?.let { it.startActivity(ActivityDestinations.userPreferences(it)) }
		}
	}
}

@Composable
private fun RailButton(
	@DrawableRes icon: Int,
	@StringRes label: Int,
	modifier: Modifier = Modifier,
	active: Boolean = false,
	onClick: () -> Unit,
) {
	IconButton(
		onClick = onClick,
		modifier = modifier,
		colors = if (active) {
			ButtonDefaults.colors(
				containerColor = JellyfinTheme.colorScheme.buttonActive,
				contentColor = JellyfinTheme.colorScheme.onButtonActive,
			)
		} else {
			ButtonDefaults.colors()
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(icon),
			contentDescription = stringResource(label),
		)
	}
}
