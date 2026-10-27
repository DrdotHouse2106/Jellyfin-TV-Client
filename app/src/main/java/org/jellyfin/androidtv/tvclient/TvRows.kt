package org.jellyfin.androidtv.tvclient

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch

/**
 * Jellyfin-TV-Client: Reihen nur so weit scrollen, wie nötig (statt das ausgewählte Element zu
 * zentrieren). So bleibt das erste Element an seinem Platz, wie in den Jellyfin-Zeilen.
 */
@OptIn(ExperimentalFoundationApi::class)
private object MinimalBringIntoViewSpec : BringIntoViewSpec {
	override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
		val trailing = offset + size
		return when {
			offset >= 0 && trailing <= containerSize -> 0f
			offset < 0 || size > containerSize -> offset
			else -> trailing - containerSize
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalScroll(content: @Composable () -> Unit) =
	CompositionLocalProvider(LocalBringIntoViewSpec provides MinimalBringIntoViewSpec, content = content)

/** Wird das erste Element ausgewählt, die Reihe ganz an den Anfang scrollen (inklusive Innenabstand). */
fun androidx.compose.ui.Modifier.firstItemScroll(
	state: androidx.compose.foundation.lazy.LazyListState,
	scope: kotlinx.coroutines.CoroutineScope,
) = this.then(androidx.compose.ui.Modifier.onFocusChanged { focus ->
	if (focus.hasFocus || focus.isFocused) scope.launch {
		kotlinx.coroutines.delay(50)
		state.scrollToItem(0, 0)
	}
})

/**
 * Zuletzt gewählte Karte je Seite (Personen-, Welt-, Filmreihen-Seite). Die Seiten werden nach „Zurück“
 * neu aufgebaut und laden ihre Reihen nach – ohne Merker landete der Fokus wieder am Seitenanfang
 * (z. B. auf der Biografie) statt auf dem eben geöffneten Titel.
 */
object PageFocus {
	private val keys = mutableMapOf<String, String>()

	fun set(page: String, key: String) {
		keys[page] = key
	}

	/** Einmalig abholen – ein späteres neues Öffnen der Seite beginnt wieder oben. */
	fun take(page: String): String? = keys.remove(page)

	private val chronologicalPages = mutableSetOf<String>()

	/** Welt-Seite: zuletzt gewählte Sortierung („Chronologisch“), damit sie nach „Zurück“ erhalten bleibt. */
	fun chronological(page: String) = page in chronologicalPages

	fun setChronological(page: String, value: Boolean) {
		if (value) chronologicalPages += page else chronologicalPages -= page
	}
}

/** Reihe zur wiederherzustellenden Karte scrollen, damit sie zusammengesetzt ist und den Fokus bekommen kann. */
@Composable
fun RestoreRowScroll(state: androidx.compose.foundation.lazy.LazyListState, keys: List<String>, restoreKey: String?) {
	androidx.compose.runtime.LaunchedEffect(restoreKey, keys.size) {
		val index = if (restoreKey == null) -1 else keys.indexOf(restoreKey)
		if (index > 0) state.scrollToItem(index)
	}
}

/** Fokus auf die gemerkte Karte, sofern sie noch auf der Seite ist ([present]); sonst auf das übliche erste Element. */
suspend fun restoreFocus(restore: androidx.compose.ui.focus.FocusRequester, fallback: androidx.compose.ui.focus.FocusRequester?, present: Boolean) {
	if (present) {
		// Erst nach dem Scrollen der Reihe ist die Karte da
		kotlinx.coroutines.delay(150)
		if (runCatching { restore.requestFocus() }.isSuccess) return
	}
	if (fallback != null) runCatching { fallback.requestFocus() }
}
