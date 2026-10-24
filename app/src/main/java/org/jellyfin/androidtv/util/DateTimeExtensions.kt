package org.jellyfin.androidtv.util

import android.content.Context
import android.os.Build
import org.jellyfin.androidtv.tvclient.TimeFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Suppress("DEPRECATION")
val Context.locale: Locale
	get() = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> resources.configuration.getLocales().get(0)
		else -> resources.configuration.locale
	}

@JvmOverloads
fun Context.getDateFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = when {
	// Jellyfin-TV-Client: Datum wie gewohnt, Uhrzeit im 24-Stunden-Format („06.11.26 20:15“ statt „12:00 AM“)
	style == FormatStyle.SHORT && TimeFormat.use24Hour(this) -> DateTimeFormatter.ofPattern(
		java.time.format.DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, java.time.chrono.IsoChronology.INSTANCE, locale) + " HH:mm",
		locale,
	)
	else -> DateTimeFormatter
		.ofLocalizedDateTime(style)
		.withLocale(locale)
}

/** Jellyfin-TV-Client: nur das Datum (z. B. für Termine, deren Uhrzeit nur ein Platzhalter um Mitternacht ist). */
fun Context.getShortDateFormatter(): DateTimeFormatter = DateTimeFormatter
	.ofLocalizedDate(FormatStyle.SHORT)
	.withLocale(locale)

@JvmOverloads
fun Context.getTimeFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = when {
	// Jellyfin-TV-Client: 24-Stunden-Format erzwingen (siehe tvclient/TimeFormat.kt)
	style == FormatStyle.SHORT && TimeFormat.use24Hour(this) -> DateTimeFormatter.ofPattern("HH:mm", locale)
	else -> DateTimeFormatter
		.ofLocalizedTime(style)
		.withLocale(locale)
}
