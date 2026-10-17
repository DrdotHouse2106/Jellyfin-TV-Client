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
