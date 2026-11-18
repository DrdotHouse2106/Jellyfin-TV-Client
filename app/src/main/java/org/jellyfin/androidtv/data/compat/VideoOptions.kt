package org.jellyfin.androidtv.data.compat

class VideoOptions : AudioOptions() {
	var audioStreamIndex: Int? = null
	var subtitleStreamIndex: Int? = null
	/** Jellyfin-TV-Client: false = der Server muss das Bild neu kodieren (kein Kopieren des Videostroms). */
	var allowVideoStreamCopy: Boolean = true
}
