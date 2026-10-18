package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.koin.androidx.compose.koinViewModel

private tailrec fun Context.componentActivity(): ComponentActivity? = when (this) {
	is ComponentActivity -> this
	is ContextWrapper -> baseContext.componentActivity()
	else -> null
}

/**
 * Jellyfin-TV-Client: Solange ein Trailer im Hintergrund läuft ([active]), startet der Bildschirmschoner
 * der App nicht (sonst liefen Trailer und Schoner gleichzeitig). Liefert zurück, ob der Schoner gerade
 * trotzdem zu sehen ist – dann soll der Trailer anhalten. Außerhalb der App (System-Bildschirmschoner)
 * passiert nichts.
 */
@Composable
fun rememberScreensaverHold(active: Boolean): State<Boolean> {
	val activity = LocalContext.current.componentActivity() ?: return remember { mutableStateOf(false) }
	val tracker = koinViewModel<InteractionTrackerViewModel>(viewModelStoreOwner = activity)
	val lifecycle = LocalLifecycleOwner.current.lifecycle
	DisposableEffect(active, lifecycle) {
		val release = if (active) tracker.addLifecycleLock(lifecycle) else ({})
		onDispose { release() }
	}
	return tracker.visible.collectAsState()
}
