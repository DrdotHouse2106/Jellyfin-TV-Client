package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioCapabilities
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.enumPreference
import timber.log.Timber

/**
 * Jellyfin-TV-Client: Ton automatisch passend zum Ausgang (HDMI, Bluetooth …), bei jedem Wiedergabestart neu ermittelt.
 * Dolby Digital (Plus) und Mehrkanal gelten nur dann als direkt abspielbar, wenn der Ausgang das Format annimmt
 * (Passthrough) oder das Gerät es selbst zu PCM dekodiert und der Ausgang die Kanalzahl schafft. Sonst wandelt der
 * Server nur den Ton in AAC Stereo um – das Bild bleibt unangetastet.
 * „Kein Ton?“ im Player merkt sich Stereo für diesen Ausgang dauerhaft auf dem Gerät.
 */
object TvClientSound {
	enum class Mode { AUTO, STEREO, PASSTHROUGH }

	val modePreference = enumPreference("pref_tvclient_sound_mode", Mode.AUTO)

	val DOLBY = setOf(Codec.Audio.AC3, Codec.Audio.EAC3)

	private val ENCODINGS = listOf(
		C.ENCODING_AC3, C.ENCODING_E_AC3, C.ENCODING_E_AC3_JOC, C.ENCODING_DOLBY_TRUEHD, C.ENCODING_DTS, C.ENCODING_DTS_HD,
	)

	/** Ermittelter Ausgang: [key] identifiziert ihn für „Kein Ton?“ (Art + Gerätename). */
	data class Output(val route: String, val key: String, val encodings: Set<Int>, val maxChannels: Int) {
		val passthrough get() = fromEncodings(encodings)

		/** z. B. „HDMI, Passthrough: AC3, E-AC3; max. 6 Kanäle“ */
		fun describe(): String {
			val names = listOfNotNull(
				"AC3".takeIf { C.ENCODING_AC3 in encodings },
				"E-AC3".takeIf { C.ENCODING_E_AC3 in encodings },
				"E-AC3-JOC".takeIf { C.ENCODING_E_AC3_JOC in encodings },
				"TrueHD".takeIf { C.ENCODING_DOLBY_TRUEHD in encodings },
				"DTS".takeIf { C.ENCODING_DTS in encodings },
				"DTS-HD".takeIf { C.ENCODING_DTS_HD in encodings },
			)
			return "$route, Passthrough: ${names.joinToString().ifEmpty { "keins" }}; max. $maxChannels Kanäle"
		}
	}

	/**
	 * Was dem Server gemeldet wird. [stereo]: nur AAC/MP3 in Stereo. Sonst: [passthrough] ohne Kanalgrenze,
	 * [blocked] nie direkt, alles andere bis [maxChannels] Kanäle.
	 */
	data class Decision(val stereo: Boolean, val passthrough: Set<String>, val blocked: Set<String>, val maxChannels: Int)

	fun fromEncodings(encodings: Set<Int>): Set<String> = buildSet {
		if (C.ENCODING_AC3 in encodings) add(Codec.Audio.AC3)
		if (C.ENCODING_E_AC3 in encodings || C.ENCODING_E_AC3_JOC in encodings) add(Codec.Audio.EAC3)
		addAll(TvClientAudioOutput.fromEncodings(encodings))
	}

	/** Reine Entscheidung (testbar). [decodable]: Dolby-Codecs, die das Gerät selbst dekodiert. null = alles melden. */
	fun decide(output: Output, decodable: Set<String>, mode: Mode, noSound: Boolean): Decision? = when {
		mode == Mode.PASSTHROUGH -> null
		mode == Mode.STEREO || noSound -> Decision(true, emptySet(), emptySet(), 2)
		else -> output.passthrough.let { Decision(false, it, DOLBY - it - decodable, output.maxChannels) }
	}

	/** Für Log, Server-Protokoll und Einstellungen: was ermittelt und was gemeldet wurde. */
	@Volatile var lastReport: String? = null
		private set

	/** Entscheidung für das Geräteprofil beim Wiedergabestart (null = Durchreichen erzwungen). */
	fun current(context: Context, prefs: UserPreferences): Decision? {
		val output = probe(context)
		val mode = prefs[modePreference]
		val noSound = output.key in stereoOutputs(context)
		val decision = decide(output, decodable(), mode, noSound)
		val reported = when {
			decision == null -> "Durchreichen erzwungen"
			decision.stereo -> if (noSound) "Stereo (Kein Ton? gemerkt)" else "immer Stereo"
			else -> "direkt bis ${decision.maxChannels} Kanäle" +
				(if (decision.passthrough.isNotEmpty()) ", durchgereicht: ${decision.passthrough.sorted().joinToString()}" else "") +
				(if (decision.blocked.isNotEmpty()) ", umgewandelt: ${decision.blocked.sorted().joinToString()}" else "")
		}
		val report = "Ton: ${output.describe()} → $reported"
		if (report != lastReport) {
			lastReport = report
			ClientDiagnostics.report(report) // schreibt auch ins App-Log
		} else {
			Timber.i(report)
		}
		return decision
	}

	@Suppress("DEPRECATION")
	fun probe(context: Context): Output {
		val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			context.getSystemService(AudioManager::class.java)?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
				?.filter { priority(it.type) < Int.MAX_VALUE }?.minByOrNull { priority(it.type) }
		} else null
		val caps = runCatching { AudioCapabilities.getCapabilities(context, AudioAttributes.DEFAULT, null) }.getOrNull()
		val hdmi = device == null || priority(device.type) == HDMI
		val encodings = if (hdmi && caps != null) ENCODINGS.filter(caps::supportsEncoding).toSet() else emptySet()
		// Kanäle laut HDMI (EDID); Bluetooth, Kopfhörer und USB-Audio gelten als Stereo
		val channels = if (!hdmi) 2 else {
			val fromDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) device?.channelCounts?.maxOrNull() else null
			fromDevice ?: caps?.maxChannelCount ?: 2
		}
		val route = when {
			device == null -> "Ausgang"
			hdmi -> "HDMI"
			priority(device.type) == BLUETOOTH -> "Bluetooth"
			priority(device.type) == USB -> "USB"
			else -> "Kopfhörer"
		}
		val name = if (device != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) device.productName?.toString() else null
		return Output(route, "$route:${name.orEmpty()}", encodings, channels.coerceIn(2, 8))
	}

	private const val BLUETOOTH = 0
	private const val USB = 1
	private const val WIRED = 2
	private const val HDMI = 3

	/** Aktiver Ausgang: Bluetooth vor USB/Kopfhörer vor HDMI (ist etwas angesteckt, spielt der Ton dort). */
	private fun priority(type: Int): Int = when (type) {
		AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, 26, 27 -> BLUETOOTH // 26/27: BLE_HEADSET/BLE_SPEAKER (Android 12)
		AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> USB
		AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> WIRED
		AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, 29 -> HDMI // 29: HDMI_EARC (Android 12)
		else -> Int.MAX_VALUE
	}

	/** Dolby-Codecs, die das Gerät selbst zu PCM dekodieren kann (Hersteller-Decoder oder FFmpeg). */
	private fun decodable(): Set<String> = runCatching {
		val decoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filterNot { it.isEncoder }.flatMap { it.supportedTypes.toList() }
		buildSet {
			if (MimeTypes.AUDIO_AC3 in decoders || ffmpegSupports(MimeTypes.AUDIO_AC3)) add(Codec.Audio.AC3)
			if (MimeTypes.AUDIO_E_AC3 in decoders || ffmpegSupports(MimeTypes.AUDIO_E_AC3)) add(Codec.Audio.EAC3)
		}
	}.getOrDefault(emptySet())

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	private fun ffmpegSupports(mimeType: String): Boolean =
		androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable() && androidx.media3.decoder.ffmpeg.FfmpegLibrary.supportsFormat(mimeType)

	// „Kein Ton?“: Ausgänge, die dauerhaft Stereo bekommen (nur auf diesem Gerät gespeichert)
	private const val PREFS = "tvclient_sound"
	private const val STEREO_OUTPUTS = "stereo_outputs"

	private fun stereoOutputs(context: Context): Set<String> =
		context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(STEREO_OUTPUTS, null).orEmpty()

	/** „Kein Ton?“ gedrückt: für den aktuellen Ausgang ab jetzt Stereo. */
	fun rememberNoSound(context: Context) {
		val key = probe(context).key
		Timber.i("Ton: Kein Ton? für %s – ab jetzt Stereo", key)
		context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(STEREO_OUTPUTS, stereoOutputs(context) + key).apply()
	}

	/** Beim Ändern der Einstellung „Ton“: gemerkte „Kein Ton?“-Ausgänge vergessen. */
	fun forgetNoSound(context: Context) {
		context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(STEREO_OUTPUTS).apply()
	}
}
