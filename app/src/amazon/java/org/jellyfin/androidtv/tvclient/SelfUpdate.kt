package org.jellyfin.androidtv.tvclient

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.preference.booleanPreference

/**
 * Jellyfin-TV-Client, Amazon-Variante: kein Selbst-Update. Der Amazon Appstore verbietet Apps, die sich
 * außerhalb des Stores nachladen und installieren – Updates kommen hier ausschließlich über den Appstore.
 * Gleiche Schnittstelle wie in der GitHub-Variante, aber ohne Download, Installation und Berechtigung.
 */
object SelfUpdate {
	val autoPreference = booleanPreference("pref_tvclient_update_auto", false)

	data class Release(val version: String, val notes: String)

	/** Nie ein Update anbieten. */
	val available: StateFlow<Release?> = MutableStateFlow(null)

	/** Immer aus dem Store: die Einstellungen zeigen nur den Hinweis auf Updates über den Appstore. */
	fun isStoreInstall(context: Context): Boolean = true

	fun autoCheck(activity: Activity) = Unit

	fun manualCheck(activity: Activity) = Unit

	fun install(activity: Activity, release: Release) = Unit
}
