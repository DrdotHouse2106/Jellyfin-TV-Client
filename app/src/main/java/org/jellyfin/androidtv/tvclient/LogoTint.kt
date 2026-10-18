package org.jellyfin.androidtv.tvclient

import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * Jellyfin-TV-Client: Logos nur dann weiß einfärben, wenn sie auf dunklem Hintergrund kaum zu sehen
 * wären (schwarz, dunkelgrau, dunkelblau). Farbige und helle Logos – z. B. der rote Marvel-Kasten mit
 * ausgestanzter Schrift – bleiben, wie sie sind.
 */
object LogoTint {
	/** Grenze für den hellsten Farbkanal (0–1), gemittelt über die sichtbaren Pixel. */
	private const val DARK_LIMIT = 0.45
	private const val SAMPLE_SIZE = 48

	/** Pixel als ARGB-Ganzzahlen; durchsichtige Pixel zählen nicht. */
	fun isDark(pixels: IntArray): Boolean {
		var sum = 0.0
		var count = 0
		for (pixel in pixels) {
			val alpha = pixel ushr 24 and 0xFF
			if (alpha < 128) continue
			val r = pixel shr 16 and 0xFF
			val g = pixel shr 8 and 0xFF
			val b = pixel and 0xFF
			sum += maxOf(r, g, b) / 255.0
			count++
		}
		return count > 0 && sum / count < DARK_LIMIT
	}

	fun isDark(bitmap: Bitmap): Boolean = runCatching {
		val small = bitmap.scale(SAMPLE_SIZE, SAMPLE_SIZE)
		val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
		small.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
		isDark(pixels)
	}.getOrDefault(false)
}
