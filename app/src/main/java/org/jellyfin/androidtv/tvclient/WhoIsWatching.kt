package org.jellyfin.androidtv.tvclient

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.koin.java.KoinJavaComponent
import kotlin.time.Duration.Companion.minutes

/**
 * Jellyfin-TV-Client: „Wer schaut?“ wie bei Netflix. Beim Start der App und nach einer längeren Pause
 * (App im Hintergrund) erscheint immer die Profilauswahl – keine automatische Anmeldung des letzten
 * Profils. Die Anmeldung jedes Profils bleibt gespeichert, ein Klick auf das Profilbild genügt (keine PIN).
 * So schaut niemand aus Bequemlichkeit im Konto eines anderen, und die Empfehlungen bleiben sauber.
 */
object WhoIsWatching {
	/** So lange darf die App im Hintergrund sein, ohne dass beim Zurückkehren neu gefragt wird. */
	private val GRACE = 10.minutes

	private var backgroundSince = 0L
	private var started = false

	fun start() {
		if (started) return
		started = true
		runCatching {
			val prefs = KoinJavaComponent.get<AuthenticationPreferences>(AuthenticationPreferences::class.java)
			// Keine Passwortabfrage bei jedem Start (die gespeicherte Anmeldung bleibt) und kein Abmelden
			// beim bloßen Wechsel in eine andere Ansicht – gefragt wird über diese Klasse
			prefs[AuthenticationPreferences.alwaysAuthenticate] = false
			prefs[AuthenticationPreferences.autoLoginUserBehavior] = UserSelectBehavior.LAST_USER
		}
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			override fun onStop(owner: LifecycleOwner) {
				backgroundSince = System.currentTimeMillis()
			}
		})
	}

	/** Beim Zurückkehren in die App: war sie lange genug im Hintergrund, wieder „Wer schaut?“ zeigen. */
	fun shouldAskAgain(): Boolean {
		val since = backgroundSince
		if (since == 0L || isPlaying()) return false
		backgroundSince = 0L
		return System.currentTimeMillis() - since > GRACE.inWholeMilliseconds
	}

	private fun isPlaying(): Boolean = runCatching {
		val controller = KoinJavaComponent.get<PlaybackControllerContainer>(PlaybackControllerContainer::class.java).playbackController
		val video = controller != null && (controller.isPlaying || controller.isPaused)
		val audio = KoinJavaComponent.get<PlaybackManager>(PlaybackManager::class.java).state.playState.value == PlayState.PLAYING
		video || audio
	}.getOrDefault(false)
}
