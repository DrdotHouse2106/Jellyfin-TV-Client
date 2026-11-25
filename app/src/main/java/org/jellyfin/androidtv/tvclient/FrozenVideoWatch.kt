package org.jellyfin.androidtv.tvclient

/**
 * Jellyfin-TV-Client: erkennt stehendes Bild bei laufendem Ton – der Bild-Decoder gibt keine Bilder mehr aus, die
 * Position läuft aber mit der Ton-Uhr weiter (z. B. AV1 auf Amlogic-Sticks: nach wenigen Bildern ist Schluss, ohne
 * Fehlermeldung, sporadisch nach Start oder Sprung). Dann lädt die App an derselben Stelle frisch; friert es beim
 * selben Titel erneut ein, startet sie mit Umwandlung des Bildes durch den Server neu.
 */
class FrozenVideoWatch {
	companion object {
		/** So lange darf das Bild bei laufender Position stehen. */
		const val FROZEN_MS = 6_000L

		/** Mindestens so weit muss die Position in dieser Zeit gelaufen sein. */
		const val MIN_PROGRESS_MS = 4_000L
	}

	private var lastFrames = -1L
	private var framesAt = 0L
	private var positionAtFrame = 0L

	fun reset() {
		lastFrames = -1L
	}

	/** Regelmäßig während der Wiedergabe aufrufen; true = Bild steht (einmal je Hänger). */
	fun check(positionMs: Long, renderedFrames: Long, now: Long): Boolean {
		if (renderedFrames != lastFrames || lastFrames < 0) {
			lastFrames = renderedFrames
			framesAt = now
			positionAtFrame = positionMs
			return false
		}
		if (now - framesAt < FROZEN_MS || positionMs - positionAtFrame < MIN_PROGRESS_MS) return false
		framesAt = now
		positionAtFrame = positionMs
		return true
	}
}
