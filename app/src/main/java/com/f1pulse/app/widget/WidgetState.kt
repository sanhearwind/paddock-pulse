package com.f1pulse.app.widget

import com.f1pulse.app.core.time.SessionSelector
import com.f1pulse.app.core.time.SessionWindow
import java.time.Instant
import java.time.ZoneId

/**
 * Plain-data views used inside Glance widgets. Kept simple (no domain coupling) so
 * the widget layer can be unit-tested in isolation.
 */
data class WidgetNextRace(
    val circuitId: String,
    val raceName: String,
    val circuitName: String,
    val countryFlagCode: String?,
    val raceInstant: Instant?,
    val fp1: Instant?,
    val fp2: Instant?,
    val fp3: Instant?,
    val qualifying: Instant?,
    val sprint: Instant?,
    val sprintQualifying: Instant?,
    val displayZone: ZoneId,
    val timeZoneLabel: String,
    val sessions: List<WidgetSession>,
    val activeSession: WidgetSession?,
    val nextSession: WidgetSession?,
    val isLive: Boolean,
    val isFinished: Boolean,
) {
    val selectedSession: WidgetSession?
        get() = activeSession ?: nextSession
}

data class WidgetSession(
    val label: String,
    val shortLabel: String,
    val instant: Instant,
    val estimatedDurationSeconds: Long,
) {
    val endInstant: Instant
        get() = instant.plusSeconds(estimatedDurationSeconds)

    fun isActive(now: Instant): Boolean =
        !now.isBefore(instant) && now.isBefore(endInstant)

    fun isCompleted(now: Instant): Boolean = !now.isBefore(endInstant)
}

data class WidgetSessionSelection(
    val active: WidgetSession?,
    val next: WidgetSession?,
)

/**
 * Thin adapter over the shared [SessionSelector] so widgets and app screens apply the same
 * session-window policy. The widget-facing types stay decoupled from the domain layer.
 */
object WidgetSessionSelector {
    fun select(sessions: List<WidgetSession>, now: Instant): WidgetSessionSelection {
        val selection = SessionSelector.select(sessions, now) {
            SessionWindow(it.instant, it.endInstant)
        }
        return WidgetSessionSelection(active = selection.active, next = selection.next)
    }
}

data class WidgetDriverStanding(
    val pos: Int,
    val code: String,
    val name: String,
    val teamName: String,
    val teamHex: String?,
    val constructorId: String?,
    val points: Double,
    val driverNumber: Int? = null,
)

data class WidgetConstructorStanding(
    val pos: Int,
    val name: String,
    val hex: String?,
    val constructorId: String?,
    val points: Double,
)

/** One classification row of a completed session, ready for bitmap rendering. */
data class WidgetSessionResultRow(
    val pos: Int,
    val code: String,
    val name: String,
    val teamName: String?,
    val teamHex: String?,
    /** Best lap or race time, already formatted. Null when the driver has no time. */
    val timeText: String?,
    /** Gap to the leader, already formatted, or the lap count as a fallback. */
    val secondaryText: String?,
    /** DNF / DNS / DSQ, when applicable — takes the place of [timeText]. */
    val statusText: String?,
)

/**
 * The most recently completed session of the season, with its classification. Covers
 * practice, qualifying and race alike, since OpenF1 publishes a result for each.
 */
data class WidgetLastSession(
    val sessionLabel: String,
    val raceName: String,
    val rows: List<WidgetSessionResultRow>,
)