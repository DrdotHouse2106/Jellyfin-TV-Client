package org.jellyfin.androidtv.tvclient

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.jellyfin.androidtv.R
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Apps und Geräte-Einstellungen des Fernsehers, damit man sie direkt
 * aus Jellyfin heraus öffnen kann (Zeilen „Apps“ und „Gerät“ auf der Startseite und per Handy).
 */
object DeviceApps {
	/** Eine auf dem Gerät installierte App. */
	data class App(
		val packageName: String,
		val label: String,
	)

	/** Eine Abkürzung in die Einstellungen des Geräts. */
	enum class SettingsShortcut(
		val key: String,
		val action: String,
		@StringRes val title: Int,
		@DrawableRes val icon: Int,
	) {
		WIFI("wifi", Settings.ACTION_WIFI_SETTINGS, R.string.tvclient_settings_wifi, R.drawable.ic_tvclient_wifi),
		BLUETOOTH("bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, R.string.tvclient_settings_bluetooth, R.drawable.ic_tvclient_bluetooth),
		DISPLAY("display", Settings.ACTION_DISPLAY_SETTINGS, R.string.tvclient_settings_display, R.drawable.ic_tvclient_display),
		SOUND("sound", Settings.ACTION_SOUND_SETTINGS, R.string.tvclient_settings_sound, R.drawable.ic_tvclient_sound),
		ALL("all", Settings.ACTION_SETTINGS, R.string.tvclient_settings_all, R.drawable.ic_settings),
		;

		companion object {
			fun fromKey(key: String) = entries.find { it.key.equals(key, ignoreCase = true) }
		}
	}

	private val launcherCategories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)

	/** Alle startbaren Apps außer dieser, alphabetisch. */
	fun getApps(context: Context): List<App> {
		val pm = context.packageManager
		return launcherCategories
			.flatMap { category ->
				runCatching {
					pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
				}.getOrDefault(emptyList())
			}
			.map { it.activityInfo }
			.filter { it.packageName != context.packageName }
			.distinctBy { it.packageName }
			.map { App(it.packageName, it.loadLabel(pm).toString()) }
			.sortedBy { it.label.lowercase() }
	}

	/** Banner der App (TV-Format), sonst ihr Symbol. */
	fun getImage(context: Context, packageName: String): Drawable? {
		val pm = context.packageManager
		return runCatching {
			pm.getApplicationBanner(packageName) ?: pm.getApplicationIcon(packageName)
		}.getOrNull()
	}

	fun hasBanner(context: Context, packageName: String) =
		runCatching { context.packageManager.getApplicationBanner(packageName) != null }.getOrDefault(false)

	/** Startet die App. Gibt false zurück, wenn sie nicht (mehr) da ist. */
	fun launch(context: Context, packageName: String): Boolean {
		val pm = context.packageManager
		val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
			?: pm.getLaunchIntentForPackage(packageName)
			?: return false

		return try {
			context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			true
		} catch (err: ActivityNotFoundException) {
			Timber.w(err, "App %s konnte nicht gestartet werden", packageName)
			false
		}
	}

	/** Einstellungen, die es auf diesem Gerät gibt (Fire TV hat nicht alle). */
	fun getSettingsShortcuts(context: Context): List<SettingsShortcut> =
		SettingsShortcut.entries.filter { shortcut ->
			Intent(shortcut.action).resolveActivity(context.packageManager) != null
		}

	fun openSettings(context: Context, shortcut: SettingsShortcut): Boolean {
		val intent = Intent(shortcut.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

		return listOf(intent, fallback).any { candidate ->
			try {
				context.startActivity(candidate)
				true
			} catch (err: ActivityNotFoundException) {
				Timber.w(err, "Einstellungen %s nicht verfügbar", shortcut.key)
				false
			}
		}
	}
}
