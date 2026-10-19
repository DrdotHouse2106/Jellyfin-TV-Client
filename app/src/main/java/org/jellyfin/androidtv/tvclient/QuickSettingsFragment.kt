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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
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

	val connection by TvClientLocalAddress.status.collectAsState()
	val connectionLabel = when (connection?.route) {
		TvClientLocalAddress.Route.HOME -> stringResource(R.string.tvclient_local_using_home, connection?.address.orEmpty())
		TvClientLocalAddress.Route.INTERNET -> stringResource(R.string.tvclient_local_using_internet, connection?.address.orEmpty())
		null -> stringResource(R.string.tvclient_local_using_unknown)
	}

	// Live-TV-Erinnerungen: Kalender-Abo per QR-Code (nur wenn der Server es anbietet)
	var calendar by remember { mutableStateOf<TvClientReminders.Calendar?>(null) }
	var remindersAvailable by remember { mutableStateOf(false) }
	var qrTarget by remember { mutableStateOf("webcal") }
	LaunchedEffect(Unit) {
		remindersAvailable = TvClientReminders.isAvailable()
		if (remindersAvailable) calendar = TvClientReminders.calendar()
	}

	val on = stringResource(R.string.tvclient_quick_on)
	val off = stringResource(R.string.tvclient_quick_off)
	// „1 Min.“, „2,5 Min.“ (Zahl im Format der Sprache, ohne „.0“)
	val minuteFormat = remember { java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
	val minuteLabel = stringResource(R.string.tvclient_quick_minutes)
	fun minutes(value: Double) = minuteLabel.format(minuteFormat.format(value))

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
		// Neu lesen, wenn sich etwas geändert hat (ohne Neuaufbau, damit der Fokus auf dem Knopf bleibt)
		@Suppress("UNUSED_EXPRESSION") revision
		Text(stringResource(R.string.lbl_settings), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)

		Section(stringResource(R.string.tvclient_quick_subtitles))
		Choices(
			listOf(
				Choice(TvClientSubtitles.SMART, stringResource(R.string.tvclient_quick_subtitles_smart)),
				Choice(TvClientSubtitles.ALWAYS, stringResource(R.string.tvclient_quick_subtitles_always)),
				Choice(TvClientSubtitles.NONE, off),
				Choice(TvClientSubtitles.SERVER, stringResource(R.string.tvclient_quick_subtitles_server)),
			),
			TvClientSubtitles.mode(),
			firstFocus = first,
		) { set(TvClientSubtitles.modePreference, it) }
		if (TvClientSubtitles.mode() == TvClientSubtitles.SMART) Hint(stringResource(R.string.tvclient_quick_subtitles_hint))

		Section(stringResource(R.string.tvclient_quick_audio))
		Choices(
			listOf(
				Choice(TvClientAudio.OWN, stringResource(R.string.tvclient_quick_audio_own)),
				Choice(TvClientAudio.ORIGINAL, stringResource(R.string.tvclient_quick_audio_original)),
				Choice(TvClientAudio.SERVER, stringResource(R.string.tvclient_quick_subtitles_server)),
			),
			prefs[TvClientAudio.modePreference],
		) { set(TvClientAudio.modePreference, it) }

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

		if (remindersAvailable) {
			Section(stringResource(R.string.tvclient_calendar_title))
			Choices(
				listOf(
					Choice("webcal", stringResource(R.string.tvclient_calendar_iphone)),
					Choice("https", stringResource(R.string.tvclient_calendar_other)),
				),
				qrTarget,
			) { qrTarget = it }
			val current = calendar
			if (current == null) {
				Hint(stringResource(R.string.tvclient_calendar_loading))
			} else {
				Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
					Box(Modifier.size(220.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
						// Neuer Link → neue Bildadresse, damit kein alter QR-Code aus dem Speicher kommt
						AsyncImage(
							modifier = Modifier.fillMaxSize(),
							url = TvClientReminders.qrUrl(qrTarget)?.let { "$it&v=${current.url.hashCode()}" },
							aspectRatio = 1f,
						)
					}
					Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 520.dp)) {
						Text(stringResource(R.string.tvclient_calendar_hint), fontSize = 15.sp, color = Color(0xFFDDE1E8))
						Text(if (qrTarget == "webcal") current.webcalUrl else current.url, fontSize = 12.sp, color = Color(0xFF8A8F99), maxLines = 2, overflow = TextOverflow.Ellipsis)
						val renewed = stringResource(R.string.tvclient_calendar_renewed)
						ActionChip(stringResource(R.string.tvclient_calendar_renew), R.drawable.ic_loop) {
							scope.launch {
								TvClientReminders.calendar(renew = true)?.let {
									calendar = it
									android.widget.Toast.makeText(context, renewed, android.widget.Toast.LENGTH_SHORT).show()
								}
							}
						}
					}
				}
			}
		}

		// Heimnetz: lokale Server-Adresse statt Umweg über das Internet
		Section(stringResource(R.string.tvclient_local_title))
		Text(connectionLabel, fontSize = 16.sp, color = Color(0xFFB3B3B3), modifier = Modifier.padding(bottom = 10.dp))
		ActionChip(stringResource(R.string.tvclient_local_edit), R.drawable.ic_house) {
			val serverId = sessionRepository.currentSession.value?.serverId ?: return@ActionChip
			editLocalAddress(context, serverId)
		}
		Hint(stringResource(R.string.tvclient_local_hint))

		Section(stringResource(R.string.tvclient_quick_more))
		ActionChip(stringResource(R.string.tvclient_quick_more_button), R.drawable.ic_settings) {
			context.startActivity(ActivityDestinations.userPreferences(context))
		}

		// Updates direkt aus den GitHub-Releases
		Section(stringResource(R.string.tvclient_update_title))
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
			ActionChip(stringResource(R.string.tvclient_update_check), R.drawable.ic_loop) {
				activityOf(context)?.let { SelfUpdate.manualCheck(it) }
			}
		}
		Section(stringResource(R.string.tvclient_update_auto))
		Choices(listOf(Choice(true, on), Choice(false, off)), prefs[SelfUpdate.autoPreference]) { set(SelfUpdate.autoPreference, it) }
		Hint(stringResource(R.string.tvclient_update_hint))

		Section(stringResource(R.string.pref_about_title))
		Text("Quallenkino ${BuildConfig.VERSION_NAME}", fontSize = 16.sp, color = Color(0xFFB3B3B3))
		Text(connectionLabel, fontSize = 14.sp, color = Color(0xFF8A8F99), modifier = Modifier.padding(top = 4.dp))
		Text(stringResource(R.string.tvclient_quick_tmdb), fontSize = 14.sp, color = Color(0xFF8A8F99), modifier = Modifier.padding(top = 4.dp))
		Spacer(Modifier.height(48.dp))
	}
}

/** Eingabe der Heimadresse (klassischer Dialog, damit die Bildschirmtastatur zuverlässig erscheint). */
private fun editLocalAddress(context: android.content.Context, serverId: java.util.UUID) {
	val input = android.widget.EditText(context).apply {
		setText(TvClientLocalAddress.manual(serverId).orEmpty())
		hint = context.getString(R.string.tvclient_local_example)
		inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
		setSingleLine()
		setSelection(text.length)
	}
	android.app.AlertDialog.Builder(context)
		.setTitle(R.string.tvclient_local_edit)
		.setMessage(R.string.tvclient_local_dialog)
		.setView(input)
		.setPositiveButton(R.string.lbl_save) { _, _ -> TvClientLocalAddress.setManual(serverId, input.text.toString()) }
		.setNeutralButton(R.string.tvclient_local_remove) { _, _ -> TvClientLocalAddress.setManual(serverId, null) }
		.setNegativeButton(R.string.lbl_cancel, null)
		.show()
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
