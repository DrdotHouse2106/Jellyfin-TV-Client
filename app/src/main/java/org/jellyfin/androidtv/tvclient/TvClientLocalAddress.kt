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
 * Sicherheit: Sie gilt nur in dem Netz, in dem sie eingetragen bzw. bestätigt wurde (Kennung aus
 * Gateway, DNS und Domain) – in einem fremden Netz mit derselben privaten IP geht der Token nie an ein
 * fremdes Gerät. Automatisch erkannte Adressen gibt es nicht mehr. Zusätzlich muss die Adresse schnell
 * (700 ms) antworten und dieselbe Server-Id melden. Anmeldung und Token bleiben gleich.
 * Die Sitzung startet ohne Wartezeit (mit dem letzten Ergebnis für dieses Netz); geprüft wird im
 * Hintergrund beim Start, beim Zurückkehren in die App und bei einem Netzwechsel, nie während einer
 * laufenden Wiedergabe.
 */
object TvClientLocalAddress {
	enum class Route { HOME, INTERNET }

	data class Status(val route: Route, val address: String)

	private const val PREFS = "tvclient_local_address"
	private const val MANUAL = "manual_"
	private const val DETECTED = "detected_"
	private const val NETWORKS = "networks_"
	private const val PROBE = "probe_"
	private const val TIMEOUT_MS = 700
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

	/** Netze (Kennung), in denen der Benutzer die Heimadresse bestätigt hat. */
	private fun confirmedNetworks(serverId: UUID): Set<String> = runCatching {
		val prefs = prefs()
		// Übernahme aus Versionen vor 1.28: eine schon eingetragene Adresse gilt im aktuellen Netz als bestätigt
		if (!prefs.contains(NETWORKS + serverId) && prefs.contains(MANUAL + serverId)) {
			currentNetwork()?.let { prefs.edit().putStringSet(NETWORKS + serverId, setOf(it)).apply() }
		}
		prefs.getStringSet(NETWORKS + serverId, null)
	}.getOrNull().orEmpty()

	/**
	 * Heimadresse speichern (leer = entfernen) und für das aktuelle Netz bestätigen. Nur in bestätigten
	 * Netzen wird sie benutzt – so geht der Token nie an ein fremdes Gerät mit derselben privaten IP.
	 */
	fun setManual(serverId: UUID, input: String?) {
		val address = normalizeAddress(input)
		val network = currentNetwork()
		prefs().edit().apply {
			remove(DETECTED + serverId)
			if (address == null) {
				remove(MANUAL + serverId)
				remove(NETWORKS + serverId)
			} else {
				// Neue Adresse: nur das aktuelle Netz gilt; gleiche Adresse: dieses Netz zusätzlich bestätigen
				val known = if (address == manual(serverId)) confirmedNetworks(serverId) else emptySet()
				putString(MANUAL + serverId, address)
				putStringSet(NETWORKS + serverId, known + listOfNotNull(network))
			}
		}.apply()
		clearProbes()
		recheck()
	}

	/** Ist die Heimadresse in dem Netz bestätigt, in dem das Gerät gerade ist? */
	fun isConfirmedHere(serverId: UUID): Boolean {
		val network = currentNetwork() ?: return false
		return network in confirmedNetworks(serverId)
	}

	/** Ergebnis der letzten Prüfung pro (Server, Netz), gespeichert: spart beim Start das Warten. */
	private fun lastProbeOk(key: String) = runCatching { prefs().getBoolean(PROBE + key, false) }.getOrDefault(false)

	private fun rememberProbe(key: String, ok: Boolean) = runCatching { prefs().edit().putBoolean(PROBE + key, ok).apply() }

	private fun clearProbes() = runCatching {
		val prefs = prefs()
		prefs.edit().apply { prefs.all.keys.filter { it.startsWith(PROBE) }.forEach(::remove) }.apply()
	}

	/**
	 * Adresse für den Sitzungsstart – ohne Netzwerkzugriff: die Heimadresse nur, wenn sie in diesem Netz
	 * bestätigt ist und die letzte Prüfung hier erfolgreich war. Geprüft wird danach im Hintergrund ([recheck]).
	 */
	fun initialAddress(serverId: UUID, remote: String): String {
		val manual = manual(serverId) ?: return use(Route.INTERNET, remote)
		val network = currentNetwork()
		if (network == null || network !in confirmedNetworks(serverId)) return use(Route.INTERNET, remote)
		return if (lastProbeOk(cacheKey(serverId, network))) use(Route.HOME, manual) else use(Route.INTERNET, remote)
	}

	private fun cacheKey(serverId: UUID, network: String) = "$serverId|$network"

	/**
	 * Adresse für den Server wählen: die eingetragene Heimadresse, wenn das Gerät in einem dafür bestätigten
	 * Netz ist, sie schnell antwortet und dieselbe Server-Id meldet – sonst die gespeicherte Adresse.
	 */
	suspend fun choose(serverId: UUID, remote: String, token: String, deviceId: String): String = withContext(Dispatchers.IO) {
		val manual = manual(serverId)
		val network = currentNetwork()
		if (manual == null || network == null || network !in confirmedNetworks(serverId) || manual.trimEnd('/') == remote.trimEnd('/')) {
			return@withContext use(Route.INTERNET, remote)
		}
		val ok = sameId(probe(manual)?.optString("Id"), serverId.toString()) && identityConfirmed(manual, serverId, token, deviceId)
		rememberProbe(cacheKey(serverId, network), ok)
		if (ok) use(Route.HOME, manual) else use(Route.INTERNET, remote)
	}

	/**
	 * Identitätsnachweis (Plugin Local Recommendations ab 12.35): Nur der echte Server kennt den Token des
	 * Geräts und kann zur Zufallszahl den passenden Nachweis berechnen – erst dann geht der Token über diese
	 * Adresse. Der Host ist Teil des Nachweises (Schutz gegen Weiterreichen an den echten Server).
	 * Ältere Plugins (404): wie bisher nur Adresse + Netzkennung + Server-Id. Jeder andere Fehler: nein.
	 */
	private fun identityConfirmed(address: String, serverId: UUID, token: String, deviceId: String): Boolean = runCatching {
		val nonce = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
		val connection = URL(address.trimEnd('/') + "/LocalRecs/Identity/Challenge?nonce=$nonce").openConnection() as HttpURLConnection
		connection.connectTimeout = TIMEOUT_MS
		connection.readTimeout = TIMEOUT_MS
		connection.setRequestProperty("Accept", "application/json")
		connection.setRequestProperty("X-Device-Id", deviceId)
		try {
			when (connection.responseCode) {
				HttpURLConnection.HTTP_NOT_FOUND -> true.also { Timber.i("Identitätsnachweis: Plugin zu alt, nur Server-Id geprüft") }
				HttpURLConnection.HTTP_OK -> {
					val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
					val proofs = body.optJSONArray("Proofs")?.let { list -> (0 until list.length()).map { list.optString(it) } }.orEmpty()
					val host = hostHeader(address) ?: return@runCatching false
					sameId(body.optString("ServerId"), serverId.toString()) &&
						verifyProof(token, nonce, serverId.toString().replace("-", "").lowercase(), host, proofs)
				}
				else -> false.also { Timber.i("Identitätsnachweis abgelehnt: HTTP %d", connection.responseCode) }
			}
		} finally {
			connection.disconnect()
		}
	}.onFailure { Timber.d("Identitätsnachweis nicht möglich: %s", it.message) }.getOrDefault(false)

	/** Host wie im Host-Header: klein, mit Port, Standardport (80/443) ohne Angabe. */
	fun hostHeader(address: String): String? {
		val uri = runCatching { URI(address) }.getOrNull() ?: return null
		val host = uri.host?.lowercase() ?: return null
		val default = if (uri.scheme == "https") 443 else 80
		return if (uri.port == -1 || uri.port == default) host else "$host:${uri.port}"
	}

	/** Eigenen Nachweis berechnen und in konstanter Zeit mit den Nachweisen des Servers vergleichen. */
	fun verifyProof(token: String, nonce: String, serverId: String, host: String, proofs: List<String>): Boolean {
		val expected = proof(token, nonce, serverId, host).toByteArray()
		var found = false
		for (candidate in proofs) {
			if (java.security.MessageDigest.isEqual(expected, candidate.lowercase().toByteArray())) found = true
		}
		return found
	}

	/** hex(HMAC-SHA256(key = SHA-256(Token), msg = nonce|ServerId|host)) */
	fun proof(token: String, nonce: String, serverId: String, host: String): String {
		val key = java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
		val mac = javax.crypto.Mac.getInstance("HmacSHA256").apply { init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")) }
		return mac.doFinal("$nonce|$serverId|$host".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
	}

	/**
	 * Kennung des aktuellen Netzes aus Gateway, DNS-Servern und Domain (als Prüfsumme). Ein anderes Netz
	 * mit derselben privaten Adresse hat fast immer eine andere Kennung.
	 */
	private fun currentNetwork(): String? = runCatching {
		val context = KoinJavaComponent.get<Context>(Context::class.java)
		val connectivity = context.getSystemService(ConnectivityManager::class.java)
		val properties = connectivity.getLinkProperties(connectivity.activeNetwork) ?: return null
		val gateway = properties.routes.filter { it.isDefaultRoute }.mapNotNull { it.gateway?.hostAddress }.sorted()
		val dns = properties.dnsServers.mapNotNull { it.hostAddress }.sorted()
		networkKey(gateway, dns, properties.domains)
	}.getOrNull()

	/** Kennung aus Gateway, DNS und Domain (testbar). Ohne Gateway keine Kennung. */
	fun networkKey(gateways: List<String>, dns: List<String>, domains: String?): String? {
		if (gateways.isEmpty()) return null
		val raw = gateways.joinToString(",") + "|" + dns.joinToString(",") + "|" + domains.orEmpty()
		return java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
			.joinToString("") { "%02x".format(it) }.take(32)
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
	}.onFailure { Timber.d("Heimadresse nicht erreichbar: %s", it.message) }.getOrNull()

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
				val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
				val address = choose(session.serverId, server.address, session.accessToken, api.deviceInfo.id)
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
