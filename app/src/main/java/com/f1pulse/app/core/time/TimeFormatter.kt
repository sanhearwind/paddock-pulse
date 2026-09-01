package com.f1pulse.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralised UTC → local timezone formatting. User requirement #3 lives here.
 * Every UI surface and widget goes through these helpers.
 */
object TimeFormatter {

    private val sessionFmt = DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm", Locale.getDefault())
    private val fullFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy • HH:mm z", Locale.getDefault())
    private val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val dateLongFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val jolpicaFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX", Locale.ROOT)

    /** Jolpica returns `date="2026-05-24"` and `time="13:00:00Z"` (may be null). */
    fun parseJolpica(date: String?, time: String?): Instant? {
        if (date.isNullOrBlank()) return null
        if (time.isNullOrBlank()) {
            return runCatching {
                LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant()
            }.getOrNull()
        }
        return runCatching {
            LocalDateTime.parse("$date $time", jolpicaFmt).toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    /** OpenF1 returns full ISO-8601 with offset, e.g. `2026-05-24T13:00:00+00:00`. */
    fun parseIso(iso: String?): Instant? = iso?.let {
        runCatching { Instant.parse(it) }
            .recoverCatching { ZonedDateTime.parse(iso).toInstant() }
            .getOrNull()
    }

    fun formatSessionTime(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(sessionFmt)

    fun formatFullDateTime(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(fullFmt)

    fun formatDateOnly(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(dateFmt)

    fun formatDateLong(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(dateLongFmt)

    fun formatTimeOnly(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(timeFmt)

    fun formatWeekday(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(weekdayFmt)

    /**
     * Formats a session duration in seconds the way timing screens do: `1:17.939` for a lap,
     * `1:23:06.801` once it runs past an hour (a race distance). Uses a fixed locale so the
     * decimal separator is always a dot.
     */
    fun formatLapDuration(seconds: Double?): String? {
        if (seconds == null || seconds <= 0.0) return null
        val totalMillis = (seconds * 1000).toLong()
        val hours = totalMillis / 3_600_000
        val minutes = (totalMillis % 3_600_000) / 60_000
        val secs = (totalMillis % 60_000) / 1000
        val millis = totalMillis % 1000
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d.%03d", hours, minutes, secs, millis)
        } else {
            String.format(Locale.US, "%d:%02d.%03d", minutes, secs, millis)
        }
    }

    /** Gap to the leader, e.g. `+0.117`. Returns null for the leader. */
    fun formatGap(seconds: Double?): String? {
        if (seconds == null || seconds <= 0.0) return null
        return String.format(Locale.US, "+%.3f", seconds)
    }

    fun now(): Instant = Instant.now()
}
