package org.jellyfin.androidtv.data.eventhandling

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.model.api.GeneralCommandType
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Fernsteuerung der App über Jellyfin-Befehle, z. B. aus „Jellyfin Remote“
 * oder Jellyfin Mobile. Steuerkreuz, OK, Zurück usw. werden als echte Tastendrücke in die App
 * gegeben, damit sie sich genau wie mit der Fernbedienung verhält (Fokus, Player, Dialoge).
 */
object RemoteNavigation {
	/** Befehle, die als Taste in die App gegeben werden. */
	val keyCommands = mapOf(
		GeneralCommandType.MOVE_UP to KeyEvent.KEYCODE_DPAD_UP,
		GeneralCommandType.MOVE_DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
		GeneralCommandType.MOVE_LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
		GeneralCommandType.MOVE_RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT,
		GeneralCommandType.SELECT to KeyEvent.KEYCODE_DPAD_CENTER,
		GeneralCommandType.BACK to KeyEvent.KEYCODE_BACK,
		GeneralCommandType.PAGE_UP to KeyEvent.KEYCODE_PAGE_UP,
		GeneralCommandType.PAGE_DOWN to KeyEvent.KEYCODE_PAGE_DOWN,
		GeneralCommandType.TOGGLE_OSD to KeyEvent.KEYCODE_MENU,
		GeneralCommandType.TOGGLE_OSD_MENU to KeyEvent.KEYCODE_MENU,
		GeneralCommandType.TOGGLE_CONTEXT_MENU to KeyEvent.KEYCODE_MENU,
		GeneralCommandType.CHANNEL_UP to KeyEvent.KEYCODE_CHANNEL_UP,
		GeneralCommandType.CHANNEL_DOWN to KeyEvent.KEYCODE_CHANNEL_DOWN,
	)

	/** Befehle, die eine Seite der App öffnen. */
	val navigationCommands = setOf(
		GeneralCommandType.GO_HOME,
		GeneralCommandType.GO_TO_SEARCH,
		GeneralCommandType.GO_TO_SETTINGS,
		GeneralCommandType.GUIDE,
	)

	val supportedCommands = keyCommands.keys + navigationCommands

	private val instrumentation by lazy { Instrumentation() }

	/**
	 * Gibt die Taste in das gerade sichtbare Fenster der App. Klappt nur, solange die App im
	 * Vordergrund ist; sonst lehnt Android das ab (dann passiert einfach nichts).
	 */
	suspend fun sendKey(keyCode: Int) = withContext(Dispatchers.IO) {
		try {
			instrumentation.sendKeyDownUpSync(keyCode)
		} catch (err: SecurityException) {
			Timber.w(err, "Taste %d konnte nicht gesendet werden (App nicht im Vordergrund?)", keyCode)
		} catch (err: RuntimeException) {
			Timber.w(err, "Taste %d konnte nicht gesendet werden", keyCode)
		}
	}

	suspend fun navigate(
		context: Context,
		navigationRepository: NavigationRepository,
		command: GeneralCommandType,
	) = withContext(Dispatchers.Main) {
		when (command) {
			GeneralCommandType.GO_HOME -> navigationRepository.reset(Destinations.home, true)
			GeneralCommandType.GO_TO_SEARCH -> navigationRepository.navigate(Destinations.search())
			GeneralCommandType.GUIDE -> navigationRepository.navigate(Destinations.liveTvGuide)
			GeneralCommandType.GO_TO_SETTINGS -> context.startActivity(
				ActivityDestinations.userPreferences(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)

			else -> Unit
		}
	}

	/** Text vom Handy: öffnet die Suche mit diesem Begriff. */
	suspend fun search(navigationRepository: NavigationRepository, text: String) = withContext(Dispatchers.Main) {
		navigationRepository.navigate(Destinations.search(text))
	}
}
