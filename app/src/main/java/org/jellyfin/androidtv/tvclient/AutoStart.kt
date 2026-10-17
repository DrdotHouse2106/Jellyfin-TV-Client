package org.jellyfin.androidtv.tvclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.preference.booleanPreference
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Quallenkino beim Einschalten des Geräts automatisch starten.
 *
 * - Nach einem Neustart (BOOT_COMPLETED) über [AutoStartReceiver].
 * - Nach dem Aufwachen aus dem Standby (SCREEN_ON) über [AutoStartService]; dieser Broadcast lässt sich
 *   nur zur Laufzeit anmelden, deshalb läuft ein schlanker Vordergrund-Dienst.
 *
 * Android 10 (SDK 29) und neuer verbieten Activity-Starts aus dem Hintergrund – dort ist die Funktion aus.
 */
object AutoStart {
	val preference = booleanPreference("pref_tvclient_autostart", false)

	/** Hintergrundstarts sind nur bis Android 9 erlaubt (Fire OS 7 = Android 9). */
	val supported: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

	fun enabled(): Boolean = supported && runCatching {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[preference]
	}.getOrDefault(false)

	/** Dienst passend zur Einstellung starten oder beenden. */
	fun sync(context: Context) {
		val intent = Intent(context, AutoStartService::class.java)
		if (enabled()) {
			runCatching { ContextCompat.startForegroundService(context, intent) }
				.onFailure { Timber.w(it, "Autostart-Dienst konnte nicht gestartet werden") }
		} else {
			context.stopService(intent)
		}
	}

	/** Startbildschirm öffnen, außer die App ist schon im Vordergrund. */
	fun launch(context: Context) {
		if (!enabled()) return
		val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
		if (inForeground) return

		Log.i(TAG, "Quallenkino wird geöffnet")
		val intent = Intent(context, StartupActivity::class.java)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
		runCatching { context.startActivity(intent) }
			.onFailure { Log.w(TAG, "Start nicht möglich", it) }
	}

	internal const val TAG = "QuallenkinoAutostart"
	internal const val EXTRA_BOOT = "boot"
}

/** Startet Quallenkino und den Dienst nach dem Hochfahren des Geräts. */
class AutoStartReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
		Log.i(AutoStart.TAG, "Gerät hochgefahren (${intent.action}), Autostart an: ${AutoStart.enabled()}")
		if (!AutoStart.enabled()) return

		// Der Dienst öffnet die App mehrmals verzögert: Fire OS legt kurz nach dem Hochfahren noch
		// seinen eigenen Startbildschirm darüber.
		val serviceIntent = Intent(context, AutoStartService::class.java).putExtra(AutoStart.EXTRA_BOOT, true)
		runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
			.onFailure {
				Log.w(AutoStart.TAG, "Dienst nicht startbar, öffne direkt", it)
				AutoStart.launch(context)
			}
	}
}

/** Lauscht auf „Bildschirm an“ (Aufwachen aus dem Standby) und öffnet dann Quallenkino. */
class AutoStartService : Service() {
	private companion object {
		const val CHANNEL_ID = "tvclient_autostart"
		const val NOTIFICATION_ID = 4711
		val BOOT_DELAYS = listOf(3_000L, 12_000L, 25_000L)
		val SCREEN_ON_DELAYS = listOf(1_500L, 6_000L, 12_000L)
	}

	private val screenOnReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == Intent.ACTION_SCREEN_ON) {
				Log.i(AutoStart.TAG, "Bildschirm an")
				scheduleLaunches(SCREEN_ON_DELAYS)
			}
		}
	}
	private var registered = false
	private val handler = Handler(Looper.getMainLooper())

	/** Mehrere Versuche, weil der Fire-TV-Startbildschirm nach dem Hochfahren/Aufwachen nachzieht. */
	private fun scheduleLaunches(delays: List<Long>) {
		handler.removeCallbacksAndMessages(null)
		delays.forEach { handler.postDelayed({ AutoStart.launch(this) }, it) }
	}

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForeground(NOTIFICATION_ID, buildNotification())

		if (!AutoStart.enabled()) {
			stopForeground(true)
			stopSelf()
			return START_NOT_STICKY
		}

		if (!registered) {
			registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
			registered = true
		}
		if (intent?.getBooleanExtra(AutoStart.EXTRA_BOOT, false) == true) scheduleLaunches(BOOT_DELAYS)
		return START_STICKY
	}

	override fun onDestroy() {
		handler.removeCallbacksAndMessages(null)
		if (registered) runCatching { unregisterReceiver(screenOnReceiver) }
		registered = false
		super.onDestroy()
	}

	@Suppress("DEPRECATION")
	private fun buildNotification(): Notification {
		val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(
				NotificationChannel(CHANNEL_ID, getString(R.string.tvclient_autostart), NotificationManager.IMPORTANCE_MIN)
			)
			Notification.Builder(this, CHANNEL_ID)
		} else {
			Notification.Builder(this).setPriority(Notification.PRIORITY_MIN)
		}

		return builder
			.setSmallIcon(R.drawable.app_icon_foreground_monochrome)
			.setContentTitle(getString(R.string.tvclient_autostart))
			.setOngoing(true)
			.build()
	}
}
