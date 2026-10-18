package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.sdk.api.client.ApiClient
import org.json.JSONObject
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID

/**
 * Jellyfin-TV-Client: Im Heimnetz direkt mit dem Server sprechen statt über die Internet-Adresse.
 *
 * Pro Server kann eine „Adresse im Heimnetz“ hinterlegt werden (z. B. http://192.168.1.10:8096).
 * Ohne Eintrag wird die vom Server gemeldete LocalAddress genutzt, falls sie eine private IP ist.
 * Die Adresse wird nur genommen, wenn sie schnell antwortet und dieselbe Server-Id meldet – sonst
 * bleibt die gespeicherte Adresse. Anmeldung und Token bleiben gleich (es ist derselbe Server).
 * Geprüft wird beim Start, beim Zurückkehren in die App und bei einem Netzwechsel, aber nie
 * während einer laufenden Wiedergabe.
 */
object TvClientLocalAddress {
	enum class Route { HOME, INTERNET }

	data class Status(val route: Route, val address: String)

	private const val PREFS = "tvclient_local_address"
	private const val MANUAL = "manual_"
	private const val DETECTED = "detected_"
	private const val TIMEOUT_MS = 1500
	private const val DEFAULT_PORT = 8096

	private val _status = MutableStateFlow<Status?>(null)
	val status = _status.asStateFlow()

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val recheckMutex = Mutex()
	private var pendingRecheck: Job? = null

	private fun prefs() = KoinJavaComponent.get<Context>(Context::class.java)
		.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

	/** Vom Benutzer eingetragene Heimadresse für diesen Server. */
	fun manual(serverId: UUID): String? = runCatching { prefs().getString(MANUAL + serverId, null) }.getOrNull()

	/** Heimadresse speichern (leer = entfernen) und sofort neu prüfen. */
	fun setManual(serverId: UUID, input: String?) {
		val address = normalizeAddress(input)
		prefs().edit().apply {
			if (address == null) remove(MANUAL + serverId) else putString(MANUAL + serverId, address)
		}.apply()
		recheck()
	}

	/**
	 * Adresse für den Server wählen: Heimadresse, wenn sie antwortet und dieselbe Id meldet,
	 * sonst die gespeicherte (Internet-)Adresse.
	 */
	suspend fun choose(serverId: UUID, remote: String): String = withContext(Dispatchers.IO) {
		val manual = manual(serverId)
		val detected = runCatching { prefs().getString(DETECTED + serverId, null) }.getOrNull()
		for (candidate in listOfNotNull(manual, detected).distinct()) {
			if (candidate.trimEnd('/') == remote.trimEnd('/')) continue
			if (sameId(probe(candidate)?.optString("Id"), serverId.toString())) return@withContext use(Route.HOME, candidate)
		}

		// Ohne eigenen Eintrag: LocalAddress des Servers versuchen, wenn sie eine private IP ist
		if (manual == null) {
			val local = probe(remote)?.optString("LocalAddress")?.let { normalizeAddress(it) }
			if (local != null && local != detected && isPrivateAddress(local) && sameId(probe(local)?.optString("Id"), serverId.toString())) {
				prefs().edit().putString(DETECTED + serverId, local).apply()
				return@withContext use(Route.HOME, local)
			}
		}

		use(Route.INTERNET, remote)
	}

	private fun use(route: Route, address: String): String {
		Timber.i("Server-Adresse: %s (%s)", address, route)
		_status.value = Status(route, address)
		return address
	}

	/** /System/Info/Public mit kurzem Timeout abfragen; null bei Fehler. */
	private fun probe(address: String): JSONObject? = runCatching {
		val connection = URL(address.trimEnd('/') + "/System/Info/Public").openConnection() as HttpURLConnection
		connection.connectTimeout = TIMEOUT_MS
		connection.readTimeout = TIMEOUT_MS
		connection.setRequestProperty("Accept", "application/json")
		try {
			if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
			JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
		} finally {
			connection.disconnect()
		}
	}.onFailure { Timber.d("Heimadresse %s nicht erreichbar: %s", address, it.message) }.getOrNull()

	/** Ids vergleichen, egal ob mit oder ohne Bindestriche. */
	fun sameId(a: String?, b: String?): Boolean {
		if (a.isNullOrBlank() || b.isNullOrBlank()) return false
		return a.replace("-", "").equals(b.replace("-", ""), ignoreCase = true)
	}

	/** Eingabe vereinheitlichen: „192.168.1.10“ → „http://192.168.1.10:8096“. Leer/ungültig → null. */
	fun normalizeAddress(input: String?): String? {
		var value = input?.trim()?.trimEnd('/') ?: return null
		if (value.isEmpty()) return null
		if (!value.contains("://")) value = "http://$value"
		val uri = runCatching { URI(value) }.getOrNull() ?: return null
		val host = uri.host ?: return null
		if (uri.scheme != "http" && uri.scheme != "https") return null
		if (host == "localhost" || host == "::1" || host == "[::1]" || host.startsWith("127.")) return null
		val port = if (uri.port == -1 && uri.scheme == "http") ":$DEFAULT_PORT" else if (uri.port == -1) "" else ":${uri.port}"
		val path = uri.rawPath.orEmpty().trimEnd('/')
		// URI.host enthält bei IPv6 schon die eckigen Klammern
		return "${uri.scheme}://$host$port$path"
	}

	/** Private IPv4-Adresse (10.x, 172.16–31.x, 192.168.x)? */
	fun isPrivateAddress(address: String): Boolean {
		val host = runCatching { URI(address).host }.getOrNull() ?: return false
		val parts = host.split('.').mapNotNull { it.toIntOrNull() }
		if (parts.size != 4) return false
		return parts[0] == 10 || (parts[0] == 172 && parts[1] in 16..31) || (parts[0] == 192 && parts[1] == 168)
	}

	/** Läuft gerade ein Video oder Musik? Dann nicht umschalten. */
	private fun isPlaying(): Boolean = runCatching {
		val controller = KoinJavaComponent.get<PlaybackControllerContainer>(PlaybackControllerContainer::class.java).playbackController
		val video = controller != null && (controller.isPlaying || controller.isPaused)
		val audio = KoinJavaComponent.get<PlaybackManager>(PlaybackManager::class.java).state.playState.value != PlayState.STOPPED
		video || audio
	}.getOrDefault(true)

	/** Adresse für die aktuelle Sitzung neu prüfen und den API-Client umstellen (nicht während der Wiedergabe). */
	fun recheck() {
		pendingRecheck?.cancel()
		pendingRecheck = scope.launch {
			// Netzwechsel kommen oft in Schüben
			delay(500)
			recheckMutex.withLock {
				if (isPlaying()) return@withLock Timber.i("Server-Adresse: Wiedergabe läuft, später prüfen")
				val session = KoinJavaComponent.get<SessionRepository>(SessionRepository::class.java).currentSession.value ?: return@withLock
				val server = KoinJavaComponent.get<AuthenticationStore>(AuthenticationStore::class.java).getServer(session.serverId) ?: return@withLock
				val address = choose(session.serverId, server.address)
				val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
				if (api.baseUrl != address && !isPlaying() && api.accessToken == session.accessToken) {
					api.update(baseUrl = address)
				}
			}
		}
	}

	private var started = false

	/** Beim Zurückkehren in die App und bei Netzwechsel neu prüfen. */
	fun start(context: Context) {
		if (started) return
		started = true
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			override fun onStart(owner: LifecycleOwner) = recheck()
		})
		runCatching {
			val connectivity = context.getSystemService(ConnectivityManager::class.java)
			connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
				override fun onAvailable(network: Network) = recheck()
				override fun onLost(network: Network) = recheck()
			})
		}.onFailure { Timber.w(it, "Netzwechsel können nicht beobachtet werden") }
	}
}
