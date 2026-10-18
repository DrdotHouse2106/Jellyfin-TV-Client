package org.jellyfin.androidtv.tvclient

import android.text.format.DateUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToLong

/** Jellyfin-TV-Client: „In 2 Stunden“ für eine lokale Uhrzeit – gerundet statt abgeschnitten. */
object RelativeTime {
	private const val MINUTE = 60_000L
	private const val HOUR = 60 * MINUTE

	/** Abstand auf ganze Minuten, ab einer Stunde auf ganze Stunden gerundet. */
	fun roundedDiff(diffMs: Long): Long {
		val unit = if (abs(diffMs) >= HOUR - MINUTE / 2) HOUR else MINUTE
		return (diffMs.toDouble() / unit).roundToLong() * unit
	}

	@JvmStatic
	fun inLocal(time: LocalDateTime): CharSequence {
		val now = Instant.now().toEpochMilli()
		val target = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
		return DateUtils.getRelativeTimeSpanString(now + roundedDiff(target - now), now, 0)
	}
}
