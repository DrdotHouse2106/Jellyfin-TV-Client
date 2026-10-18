package org.jellyfin.androidtv.tvclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.util.getQuantityString
import org.jellyfin.preference.Preference
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes

/**
 * Jellyfin-TV-Client: schlanke Einstellungsseite (wie in der webOS-App). Nur das Wichtige als Auswahlreihen;
 * alles andere unter „Weitere Einstellungen …“ im bisherigen Menü.
 */
class QuickSettingsFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = content {
		JellyfinTheme {
			QuickSettings()
		}
	}
}

private class Choice<T>(val value: T, val label: String)

@Composable
private fun QuickSettings() {
	val context = LocalContext.current
	val prefs = koinInject<UserPreferences>()
	val sessionRepository = koinInject<SessionRepository>()
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()
	val backgroundService = koinInject<BackgroundService>()
	val scope = rememberCoroutineScope()
	val first = remember { FocusRequester() }
	// Bei jeder Änderung neu zeichnen (die Werte liegen in den Einstellungen)
	var revision by remember { mutableIntStateOf(0) }

	LaunchedEffect(Unit) {
		backgroundService.clearBackgrounds()
		delay(100)
		runCatching { first.requestFocus() }
	}

	fun <T : Any> set(preference: Preference<T>, value: T) {
		prefs[preference] = value
		revision++
	}

	val on = stringResource(R.string.tvclient_quick_on)
	val off = stringResource(R.string.tvclient_quick_off)
	fun minutes(value: Double) = context.getQuantityString(R.plurals.minutes, value)

	fun restart() {
		context.startActivity(ActivityDestinations.startup(context))
		activityOf(context)?.finishAfterTransition()
	}

	Column(
		Modifier
			.fillMaxSize()
			.background(Color(0xFF0B0E14))
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 64.dp, vertical = 36.dp),
	) {
		// Neu lesen, wenn sich etwas geändert hat
		key(revision) {
		Text(stringResource(R.string.lbl_settings), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)

		Section(stringResource(R.string.tvclient_quick_subtitles))
		Choices(
			listOf(Choice(true, stringResource(R.string.tvclient_quick_subtitles_smart)), Choice(false, stringResource(R.string.tvclient_quick_subtitles_server))),
			prefs[TvClientSubtitles.smartPreference],
			firstFocus = first,
		) { set(TvClientSubtitles.smartPreference, it) }
		Hint(stringResource(R.string.tvclient_quick_subtitles_hint))

		Section(stringResource(R.string.tvclient_trailer_preview))
		Choices(listOf(Choice(true, on), Choice(false, off)), prefs[TrailerPreview.enabledPreference]) { set(TrailerPreview.enabledPreference, it) }
		Section(stringResource(R.string.tvclient_quick_trailer_sound))
		Choices(listOf(Choice(true, on), Choice(false, off)), prefs[TrailerPreview.soundPreference]) { set(TrailerPreview.soundPreference, it) }

		Section(stringResource(R.string.tvclient_quick_screensaver))
		val timeouts = listOf(1.0, 2.5, 5.0, 10.0)
		val currentTimeout = if (!prefs[UserPreferences.screensaverInAppEnabled]) 0.0
		else timeouts.minByOrNull { kotlin.math.abs(it.minutes.inWholeMilliseconds - prefs[UserPreferences.screensaverInAppTimeout]) } ?: 0.0
		Choices(timeouts.map { Choice(it, minutes(it)) } + Choice(0.0, off), currentTimeout) { value ->
			if (value == 0.0) set(UserPreferences.screensaverInAppEnabled, false)
			else {
				prefs[UserPreferences.screensaverInAppEnabled] = true
				set(UserPreferences.screensaverInAppTimeout, value.minutes.inWholeMilliseconds)
			}
		}
		Section(stringResource(R.string.tvclient_screensaver_trailers))
		Choices(listOf(Choice(true, on), Choice(false, off)), prefs[ScreensaverTrailers.enabledPreference]) { set(ScreensaverTrailers.enabledPreference, it) }
		if (prefs[ScreensaverTrailers.enabledPreference]) {
			Section(stringResource(R.string.tvclient_screensaver_volume))
			Choices(
				listOf(
					Choice(0, stringResource(R.string.tvclient_volume_mute)),
					Choice(30, stringResource(R.string.tvclient_volume_quiet)),
					Choice(75, stringResource(R.string.tvclient_volume_normal)),
				),
				prefs[ScreensaverTrailers.volumePreference],
			) { set(ScreensaverTrailers.volumePreference, it) }
		}

		Section(stringResource(R.string.tvclient_jellyseerr_title))
		Choices(listOf(Choice(true, on), Choice(false, off)), prefs[Jellyseerr.enabledPreference]) { set(Jellyseerr.enabledPreference, it) }

		Section(stringResource(R.string.tvclient_settings_account))
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			ActionChip(stringResource(R.string.lbl_switch_user), R.drawable.ic_user) {
				sessionRepository.destroyCurrentSession()
				restart()
			}
			ActionChip(stringResource(R.string.lbl_sign_out), R.drawable.ic_logout) {
				scope.launch {
					val session = sessionRepository.currentSession.value
					val server = session?.let { serverRepository.getServer(it.serverId) }
					val user = server?.let { serverUserRepository.getStoredServerUsers(it).find { u -> u.id == session.userId } }
					if (user != null) authenticationRepository.logout(user)
					sessionRepository.destroyCurrentSession()
					restart()
				}
			}
			ActionChip(stringResource(R.string.tvclient_quick_servers), R.drawable.ic_users) {
				context.startActivity(ActivityDestinations.authPreferences(context))
			}
		}

		Section(stringResource(R.string.tvclient_quick_more))
		ActionChip(stringResource(R.string.tvclient_quick_more_button), R.drawable.ic_settings) {
			context.startActivity(ActivityDestinations.userPreferences(context))
		}

		Section(stringResource(R.string.pref_about_title))
		Text("Quallenkino ${BuildConfig.VERSION_NAME}", fontSize = 16.sp, color = Color(0xFFB3B3B3))
		Text(stringResource(R.string.tvclient_quick_tmdb), fontSize = 14.sp, color = Color(0xFF8A8F99), modifier = Modifier.padding(top = 4.dp))
		Spacer(Modifier.height(48.dp))
		}
	}
}

private fun activityOf(context: android.content.Context): android.app.Activity? {
	var current: android.content.Context? = context
	while (current is android.content.ContextWrapper) {
		if (current is android.app.Activity) return current
		current = current.baseContext
	}
	return null
}

@Composable
private fun Section(title: String) {
	Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9CED8), modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}

@Composable
private fun Hint(text: String) {
	Text(text, fontSize = 14.sp, color = Color(0xFF8A8F99), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun <T> Choices(choices: List<Choice<T>>, selected: T, firstFocus: FocusRequester? = null, onSelect: (T) -> Unit) {
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		choices.forEachIndexed { index, choice ->
			val modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
			Chip(choice.label, selected = choice.value == selected, icon = null, modifier = modifier) { onSelect(choice.value) }
		}
	}
}

@Composable
private fun ActionChip(label: String, icon: Int, onClick: () -> Unit) = Chip(label, selected = false, icon = icon, onClick = onClick)

/** Knopf wie in der webOS-App: hell bei Fokus, grüner Haken bei der gewählten Möglichkeit. */
@Composable
private fun Chip(label: String, selected: Boolean, icon: Int?, modifier: Modifier = Modifier, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	val shape = RoundedCornerShape(10.dp)
	val textColor = when {
		focused -> Color(0xFF0B0E14)
		selected -> Color(0xFF46D369)
		else -> Color.White
	}
	Row(
		modifier
			.widthIn(max = 420.dp)
			.background(if (focused) Color.White else Color.White.copy(alpha = 0.14f), shape)
			.border(2.dp, if (selected && !focused) Color(0xFF46D369) else Color.Transparent, shape)
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.padding(horizontal = 18.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		val iconRes = if (selected) R.drawable.ic_check else icon
		if (iconRes != null) {
			Image(painterResource(iconRes), contentDescription = null, colorFilter = ColorFilter.tint(textColor), modifier = Modifier.size(18.dp))
		}
		Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
	}
}
