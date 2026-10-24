package org.jellyfin.androidtv.tvclient

/**
 * Jellyfin-TV-Client: Altersfreigabe überall gleich schreiben – „FSK 12“ statt „12“ (z. B. bei Anime) oder
 * „FSK-16“. Andere Angaben (z. B. „PG-13“, „TV-MA“) bleiben unverändert.
 */
object ParentalRating {
	private val fsk = Regex("""^(?:DE[-/ ]?|FSK[- ]?)?(\d{1,2})$""", RegexOption.IGNORE_CASE)

	@JvmStatic
	fun display(rating: String?): String? {
		val value = rating?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		return fsk.matchEntire(value)?.let { "FSK ${it.groupValues[1]}" } ?: value
	}
}
