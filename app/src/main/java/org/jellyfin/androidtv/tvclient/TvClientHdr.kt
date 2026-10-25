package org.jellyfin.androidtv.tvclient

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

/**
 * Jellyfin-TV-Client: HDR nur direkt abspielen, wenn der **Bildschirm** es kann – nicht nur der Decoder.
 * Ein Stick an einem Fernseher ohne HDR zeigte HDR-Filme sonst zu dunkel und blass; ohne HDR-Bildschirm
 * wandelt der Server mit Tonemapping nach SDR um.
 */
object TvClientHdr {
	/** Automatisch: Bildschirm prüfen; Umwandeln: HDR immer nach SDR; Direkt: wie bisher nur der Decoder. */
	enum class HdrMode { AUTO, CONVERT, DIRECT }

	data class DisplayHdr(
		val hdr10: Boolean,
		val hdr10Plus: Boolean,
		val hlg: Boolean,
		val dolbyVision: Boolean,
	) {
		/** HDR10+ trägt HDR10 als Grundlage – ein HDR10-Bildschirm zeigt es richtig. */
		val hdr10PlusOrBase get() = hdr10Plus || hdr10

		override fun toString() = listOfNotNull(
			"HDR10".takeIf { hdr10 }, "HDR10+".takeIf { hdr10Plus }, "HLG".takeIf { hlg }, "Dolby Vision".takeIf { dolbyVision },
		).joinToString(", ").ifEmpty { "keine" }
	}

	val ALL = DisplayHdr(hdr10 = true, hdr10Plus = true, hlg = true, dolbyVision = true)
	val NONE = DisplayHdr(hdr10 = false, hdr10Plus = false, hlg = false, dolbyVision = false)

	@Volatile private var lastReported: DisplayHdr? = null

	/** HDR-Arten, die direkt abgespielt werden dürfen – bei jedem Wiedergabestart neu ermittelt. */
	fun display(context: Context, mode: HdrMode): DisplayHdr = when (mode) {
		HdrMode.CONVERT -> NONE
		HdrMode.DIRECT -> ALL
		HdrMode.AUTO -> read(context)?.also(::report) ?: ALL
	}

	/** Aus den HDR-Typen des Bildschirms (Display.HdrCapabilities; testbar). */
	fun fromTypes(types: IntArray): DisplayHdr = DisplayHdr(
		hdr10 = HDR_TYPE_HDR10 in types,
		hdr10Plus = HDR_TYPE_HDR10_PLUS in types,
		hlg = HDR_TYPE_HLG in types,
		dolbyVision = HDR_TYPE_DOLBY_VISION in types,
	)

	// Werte von Display.HdrCapabilities.HDR_TYPE_* (HDR10+ erst ab API 29)
	private const val HDR_TYPE_DOLBY_VISION = 1
	private const val HDR_TYPE_HDR10 = 2
	private const val HDR_TYPE_HLG = 3
	private const val HDR_TYPE_HDR10_PLUS = 4

	@Suppress("DEPRECATION")
	private fun read(context: Context): DisplayHdr? {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
		return runCatching {
			val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
			fromTypes(display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0))
		}.getOrNull()
	}

	/** Welche HDR-Typen der Bildschirm meldet, einmal pro Änderung ins Client-Log des Servers. */
	private fun report(hdr: DisplayHdr) {
		if (hdr == lastReported) return
		lastReported = hdr
		ClientDiagnostics.report(
			"Bildschirm meldet HDR: $hdr – " +
				if (hdr == NONE) "HDR wird vom Server nach SDR umgewandelt" else "nur diese Arten werden direkt abgespielt"
		)
	}
}
