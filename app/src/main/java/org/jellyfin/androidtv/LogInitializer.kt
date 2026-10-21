package org.jellyfin.androidtv

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

class LogInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		// Enable improved logging for leaking resources
		// https://wh0.github.io/2020/08/12/closeguard.html
		if (BuildConfig.DEBUG) {
			try {
				Class.forName("dalvik.system.CloseGuard")
					.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
					.invoke(null, true)
			} catch (e: ReflectiveOperationException) {
				@Suppress("TooGenericExceptionThrown")
				throw RuntimeException(e)
			}
		}

		// Initialize the logging library
		// Jellyfin-TV-Client: Tokens nie ins Protokoll; im Release-Build nur Hinweise ab „Info“
		Timber.plant(if (BuildConfig.DEBUG) RedactingDebugTree() else RedactingReleaseTree())
		Timber.i("Log tree planted")
	}

	override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

/** Zugangsdaten in Protokollzeilen schwärzen (api_key=, ApiKey=, Token="…", X-Emby-Token). */
fun redactSecrets(message: String): String = message
	.replace(Regex("(?i)(api_?key=)[^&\\s\"]+"), "$1***")
	.replace(Regex("(?i)(token=\\\"?)[^\"\\s,]+"), "$1***")
	.replace(Regex("(?i)(x-(emby|mediabrowser)-token[:=]\\s*)\\S+"), "$1***")

private class RedactingDebugTree : Timber.DebugTree() {
	override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = super.log(priority, tag, redactSecrets(message), t)
}

private class RedactingReleaseTree : Timber.DebugTree() {
	override fun isLoggable(tag: String?, priority: Int) = priority >= android.util.Log.INFO
	override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = super.log(priority, tag, redactSecrets(message), t)
}
