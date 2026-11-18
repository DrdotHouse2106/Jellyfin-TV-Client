package org.jellyfin.androidtv.tvclient

/**
 * Jellyfin-TV-Client: erkennt einen Player, der nach Start oder Fortsetzen im Puffern hängen bleibt – ohne Fehler
 * und ohne dass der Puffer wächst (z. B. MP4 ohne „faststart“ über einen Proxy: der Sprung mitten in die Datei
 * kommt nie an). Dann gilt das als Abspielfehler, und die App startet neu ohne Direktwiedergabe (der Server
 * liefert umgepackt bzw. umgewandelt).
 */
class BufferStallWatch {
	companion object {
		/** So lange darf das Puffern ohne jeden Fortschritt dauern. */
		const val STALL_MS = 20_000L
	}

	private var lastBuffered = -1L
	private var since = -1L

	fun reset() {
		lastBuffered = -1L
		since = -1L
	}

	/**
	 * Regelmäßig aufrufen, solange der Player puffert und abspielen soll. true = hängt (einmal je Hänger).
	 */
	fun check(bufferedMs: Long, now: Long): Boolean {
		if (bufferedMs != lastBuffered || since < 0) {
			lastBuffered = bufferedMs
			since = now
			return false
		}
		if (now - since < STALL_MS) return false
		since = now
		return true
	}

	/** Für Log und Problembericht. */
	fun describe(positionMs: Long, bufferedMs: Long, durationMs: Long) =
		"Hänger beim Puffern: Position $positionMs ms, gepuffert bis $bufferedMs ms, Länge $durationMs ms, seit ${STALL_MS / 1000} s kein Fortschritt"
}
