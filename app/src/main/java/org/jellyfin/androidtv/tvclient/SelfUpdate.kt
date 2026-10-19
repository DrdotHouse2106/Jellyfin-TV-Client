package org.jellyfin.androidtv.tvclient

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.preference.booleanPreference
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.time.Duration.Companion.hours

/**
 * Jellyfin-TV-Client: Selbst-Update ohne App-Store. Prüft die neueste Version in den GitHub-Releases,
 * lädt die APK herunter, prüft Prüfsumme und Signatur und startet die Installation (Android fragt
 * jedes Mal nach einer Bestätigung). Beim ersten Mal muss „Unbekannte Apps installieren“ für
 * Quallenkino erlaubt werden.
 */
object SelfUpdate {
	private const val REPO = "DrdotHouse2106/Jellyfin-TV-Client"
	private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
	private const val CHANGELOG_URL = "https://raw.githubusercontent.com/$REPO/main/CHANGELOG-TV.md"
	private const val APK_NAME = "quallenkino.apk"
	private const val PREFS = "tvclient_update"
	private const val LAST_CHECK = "last_check"
	private const val TIMEOUT_MS = 15_000
	private val CHECK_INTERVAL = 20.hours

	/** Beim Start automatisch nach Updates suchen (höchstens einmal am Tag). */
	val autoPreference = booleanPreference("pref_tvclient_update_auto", true)

	data class Release(val version: String, val apkUrl: String, val size: Long?, val sha256: String?, val notes: String)

	private val json = Json { ignoreUnknownKeys = true }

	/** Versionen vergleichen: „1.24.10“ > „1.24.9“; „v“ vorne und Zusätze („-beta“) werden ignoriert. */
	fun isNewer(remote: String, local: String): Boolean {
		fun parts(value: String) = value.trim().removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
		val a = parts(remote)
		val b = parts(local)
		for (i in 0 until maxOf(a.size, b.size)) {
			val x = a.getOrElse(i) { 0 }
			val y = b.getOrElse(i) { 0 }
			if (x != y) return x > y
		}
		return false
	}

	/** Abschnitte aus CHANGELOG-TV.md, die neuer als die installierte Version sind (höchstens [limit]). */
	fun changesSince(changelog: String, local: String, limit: Int = 5): String = changelog
		.split(Regex("(?m)^## "))
		.drop(1)
		.filter { section -> isNewer(section.substringBefore(' ').substringBefore('\n'), local) }
		.take(limit)
		.joinToString("\n") { section ->
			val title = section.substringBefore('\n').trim()
			val body = section.substringAfter('\n').trim()
				// Markdown etwas vereinfachen
				.replace("**", "")
				.replace(Regex("`([^`]*)`"), "$1")
				.replace(Regex("(?m)^- "), "• ")
			"$title\n$body\n"
		}
		.trim()

	private fun get(url: String): String {
		val connection = URL(url).openConnection() as HttpURLConnection
		return try {
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS
			connection.setRequestProperty("Accept", "application/vnd.github+json")
			connection.setRequestProperty("User-Agent", "Quallenkino/${BuildConfig.VERSION_NAME}")
			if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
			connection.inputStream.bufferedReader().use { it.readText() }
		} finally {
			connection.disconnect()
		}
	}

	/** Neueste Version, wenn sie neuer als die installierte ist; sonst null. */
	suspend fun check(): Release? = withContext(Dispatchers.IO) {
		val release = json.parseToJsonElement(get(LATEST_URL)) as? JsonObject ?: return@withContext null
		val tag = (release["tag_name"] as? JsonPrimitive)?.content ?: return@withContext null
		if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext null
		val asset = (release["assets"] as? JsonArray).orEmpty()
			.mapNotNull { it as? JsonObject }
			.firstOrNull { (it["name"] as? JsonPrimitive)?.content == APK_NAME }
			?: return@withContext null
		val url = (asset["browser_download_url"] as? JsonPrimitive)?.content ?: return@withContext null
		val digest = (asset["digest"] as? JsonPrimitive)?.content?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
		val notes = runCatching { changesSince(get(CHANGELOG_URL), BuildConfig.VERSION_NAME) }.getOrDefault("")
		Release(tag.removePrefix("v"), url, (asset["size"] as? JsonPrimitive)?.longOrNull, digest, notes)
	}

	private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

	private fun isPlaying(): Boolean = runCatching {
		val controller = KoinJavaComponent.get<PlaybackControllerContainer>(PlaybackControllerContainer::class.java).playbackController
		controller != null && (controller.isPlaying || controller.isPaused)
	}.getOrDefault(false)

	/** Beim Start: höchstens einmal am Tag still prüfen und nur bei einer neuen Version fragen. */
	fun autoCheck(activity: Activity) {
		val owner = activity as? LifecycleOwner ?: return
		val enabled = runCatching {
			KoinJavaComponent.get<org.jellyfin.androidtv.preference.UserPreferences>(org.jellyfin.androidtv.preference.UserPreferences::class.java)[autoPreference]
		}.getOrDefault(true)
		if (!enabled) return
		val now = System.currentTimeMillis()
		if (now - prefs(activity).getLong(LAST_CHECK, 0) < CHECK_INTERVAL.inWholeMilliseconds) return
		owner.lifecycleScope.launch {
			// Erst die Startseite in Ruhe aufbauen lassen
			delay(8_000)
			if (isPlaying() || activity.isFinishing) return@launch
			val release = runCatching { check() }.onFailure { Timber.w(it, "Update-Prüfung fehlgeschlagen") }.getOrNull()
			prefs(activity).edit().putLong(LAST_CHECK, System.currentTimeMillis()).apply()
			if (release != null && !isPlaying() && !activity.isFinishing) offer(activity, release)
		}
	}

	/** „Nach Updates suchen“ in den Einstellungen. */
	fun manualCheck(activity: Activity) {
		val owner = activity as? LifecycleOwner ?: return
		Toast.makeText(activity, R.string.tvclient_update_checking, Toast.LENGTH_SHORT).show()
		owner.lifecycleScope.launch {
			val result = runCatching { check() }
			prefs(activity).edit().putLong(LAST_CHECK, System.currentTimeMillis()).apply()
			result.onSuccess { release ->
				if (release == null) Toast.makeText(activity, activity.getString(R.string.tvclient_update_current, BuildConfig.VERSION_NAME), Toast.LENGTH_LONG).show()
				else offer(activity, release)
			}.onFailure {
				Timber.w(it, "Update-Prüfung fehlgeschlagen")
				Toast.makeText(activity, R.string.tvclient_update_failed, Toast.LENGTH_LONG).show()
			}
		}
	}

	/** Hinweis „Update verfügbar“ mit den Änderungen. */
	fun offer(activity: Activity, release: Release) {
		AlertDialog.Builder(activity)
			.setTitle(activity.getString(R.string.tvclient_update_available, release.version))
			.setMessage(release.notes.ifBlank { activity.getString(R.string.tvclient_update_no_notes) })
			.setPositiveButton(R.string.tvclient_update_install) { _, _ -> install(activity, release) }
			.setNegativeButton(R.string.tvclient_update_later, null)
			.show()
	}

	/** Herunterladen (mit Fortschritt), prüfen und installieren. */
	private fun install(activity: Activity, release: Release) {
		val owner = activity as? LifecycleOwner ?: return
		if (!canInstall(activity)) return requestInstallPermission(activity)

		val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
		val label = TextView(activity).apply { gravity = Gravity.CENTER }
		val layout = LinearLayout(activity).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(64, 32, 64, 16)
			addView(progress)
			addView(label)
		}
		val dialog = AlertDialog.Builder(activity)
			.setTitle(activity.getString(R.string.tvclient_update_downloading, release.version))
			.setView(layout)
			.setCancelable(false)
			.show()

		owner.lifecycleScope.launch {
			val file = runCatching {
				download(activity, release) { percent ->
					launch(Dispatchers.Main) {
						progress.progress = percent
						label.text = "$percent %"
					}
				}
			}.onFailure { Timber.w(it, "Update konnte nicht geladen werden") }.getOrNull()
			dialog.dismiss()
			when {
				file == null -> Toast.makeText(activity, R.string.tvclient_update_failed, Toast.LENGTH_LONG).show()
				!sameSignature(activity, file) -> {
					file.delete()
					Toast.makeText(activity, R.string.tvclient_update_signature, Toast.LENGTH_LONG).show()
				}
				else -> startInstaller(activity, file)
			}
		}
	}

	private suspend fun download(context: Context, release: Release, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
		val dir = File(context.cacheDir, "updates").apply { mkdirs() }
		// Alte Downloads aufräumen
		dir.listFiles()?.forEach { it.delete() }
		val target = File(dir, "quallenkino-${release.version}.apk")
		val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
		try {
			connection.connectTimeout = TIMEOUT_MS
			connection.readTimeout = TIMEOUT_MS * 2
			connection.instanceFollowRedirects = true
			if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
			val total = release.size ?: connection.contentLengthLong.takeIf { it > 0 }
			val digest = MessageDigest.getInstance("SHA-256")
			var loaded = 0L
			var lastPercent = -1
			connection.inputStream.use { input ->
				target.outputStream().use { output ->
					val buffer = ByteArray(64 * 1024)
					while (true) {
						val read = input.read(buffer)
						if (read < 0) break
						output.write(buffer, 0, read)
						digest.update(buffer, 0, read)
						loaded += read
						val percent = if (total != null) (loaded * 100 / total).toInt().coerceIn(0, 100) else 0
						if (percent != lastPercent) {
							lastPercent = percent
							onProgress(percent)
						}
					}
				}
			}
			if (release.size != null && loaded != release.size) error("Größe stimmt nicht ($loaded statt ${release.size})")
			val actual = digest.digest().joinToString("") { "%02x".format(it) }
			if (release.sha256 != null && !actual.equals(release.sha256, ignoreCase = true)) {
				target.delete()
				error("Prüfsumme stimmt nicht")
			}
			target
		} finally {
			connection.disconnect()
		}
	}

	@Suppress("DEPRECATION")
	private fun signatures(info: PackageInfo?): Set<String> {
		if (info == null) return emptySet()
		val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			info.signingInfo?.apkContentsSigners?.toList().orEmpty()
		} else {
			info.signatures?.toList().orEmpty()
		}
		return list.map { it.toCharsString() }.toSet()
	}

	/** Gleiche App und gleiche Signatur wie die installierte – sonst würde Android das Update ablehnen. */
	@Suppress("DEPRECATION")
	private fun sameSignature(context: Context, file: File): Boolean = runCatching {
		val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
		val archive = context.packageManager.getPackageArchiveInfo(file.path, flags) ?: return false
		if (archive.packageName != context.packageName) return false
		val installed = context.packageManager.getPackageInfo(context.packageName, flags)
		val expected = signatures(installed)
		val actual = signatures(archive)
		// Manche Geräte liefern für Archive keine Signaturen; dann entscheidet der Installer selbst
		actual.isEmpty() || actual == expected
	}.getOrDefault(true)

	private fun canInstall(context: Context): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

	/** Einmalig „Unbekannte Apps installieren“ für Quallenkino erlauben (Fire TV: „Apps unbekannter Herkunft“). */
	private fun requestInstallPermission(activity: Activity) {
		AlertDialog.Builder(activity)
			.setTitle(R.string.tvclient_update_permission_title)
			.setMessage(R.string.tvclient_update_permission)
			.setPositiveButton(R.string.tvclient_update_permission_open) { _, _ ->
				val intents = buildList {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						add(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
					}
					add(Intent(Settings.ACTION_SECURITY_SETTINGS))
					add(Intent(Settings.ACTION_SETTINGS))
				}
				val opened = intents.any { intent ->
					try {
						activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
						true
					} catch (_: ActivityNotFoundException) {
						false
					} catch (_: SecurityException) {
						false
					}
				}
				if (!opened) Toast.makeText(activity, R.string.tvclient_update_permission, Toast.LENGTH_LONG).show()
			}
			.setNegativeButton(R.string.lbl_cancel, null)
			.show()
	}

	private fun startInstaller(activity: Activity, file: File) {
		val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
		@Suppress("DEPRECATION")
		val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
			.setDataAndType(uri, "application/vnd.android.package-archive")
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			activity.startActivity(intent)
		} catch (_: ActivityNotFoundException) {
			activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
		}
	}
}
