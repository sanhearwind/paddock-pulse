package com.f1pulse.app.widget

import android.content.Context
import com.f1pulse.app.R
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.SessionSelector
import com.f1pulse.app.core.time.SessionWindow
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.core.time.chronologicalSchedule
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.domain.model.SessionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

/**
 * Shared helper for the Glance widgets. Loads widget-ready data from the
 * domain layer via [WidgetEntryPoint] and converts it to the simple [WidgetNextRace] /
 * [WidgetDriverStanding] / [WidgetConstructorStanding] types.
 */
object WidgetDataLoader {

    /** Upcoming schedule always belongs to the current calendar season. */
    fun scheduleSeason(now: java.time.LocalDate = java.time.LocalDate.now()): Int = now.year

    /** Standings may fall back to the completed season before the new championship begins. */
    fun standingsSeason(now: java.time.LocalDate = java.time.LocalDate.now()): Int =
        if (now.monthValue < 3) now.year - 1 else now.year

    suspend fun loadNextRace(context: Context, season: Int): WidgetNextRace? = withContext(Dispatchers.IO) {
        val ep = WidgetEntryPointAccess.from(context)
        val race = ep.getNextRaceUseCase().invoke(season).first() ?: return@withContext null
        val raceInstant = race.sessions.race
        val now = Instant.now()
        val settings = ep.settingsDataStore().flow.first()
        val displayZone = ep.timeZoneResolver().resolve(settings.timeZoneMode, race.circuit.circuitId)
        val timeZoneLabel = when (settings.timeZoneMode) {
            TimeZoneMode.DEVICE -> "设备时区 · ${displayZone.id}"
            TimeZoneMode.RACE_LOCAL -> "比赛当地时间 · ${displayZone.id}"
        }
        val sessions = race.sessions.chronologicalSchedule().map { session ->
            when (session.type) {
                SessionType.FP1 -> WidgetSession("FP1", "FP1", session.instant, 60 * 60)
                SessionType.FP2 -> WidgetSession("FP2", "FP2", session.instant, 60 * 60)
                SessionType.FP3 -> WidgetSession("FP3", "FP3", session.instant, 60 * 60)
                SessionType.SPRINT_QUALIFYING -> WidgetSession("SPRINT QUALI", "SQ", session.instant, 60 * 60)
                SessionType.SPRINT -> WidgetSession("SPRINT", "S", session.instant, 60 * 60)
                SessionType.QUALIFYING -> WidgetSession("QUALI", "Q", session.instant, 90 * 60)
                SessionType.RACE -> WidgetSession("RACE", "R", session.instant, 3 * 60 * 60)
            }
        }
        val selection = WidgetSessionSelector.select(sessions, now)
        WidgetNextRace(
            circuitId = race.circuit.circuitId,
            raceName = race.raceName,
            circuitName = race.circuit.name,
            countryFlagCode = CountryFlags.fromLocation(race.circuit.country),
            raceInstant = raceInstant,
            fp1 = race.sessions.fp1,
            fp2 = sessions.firstOrNull { it.shortLabel == "FP2" }?.instant,
            fp3 = sessions.firstOrNull { it.shortLabel == "FP3" }?.instant,
            qualifying = race.sessions.qualifying,
            sprint = race.sessions.sprint,
            sprintQualifying = race.sessions.sprintQualifying,
            displayZone = displayZone,
            timeZoneLabel = timeZoneLabel,
            sessions = sessions,
            activeSession = selection.active,
            nextSession = selection.next,
            isLive = selection.active != null,
            isFinished = race.isFinished,
        )
    }

    suspend fun loadTopDrivers(context: Context, season: Int, limit: Int = 5): List<WidgetDriverStanding> = withContext(Dispatchers.IO) {
        val ep = WidgetEntryPointAccess.from(context)
        val standings = ep.getDriverStandingsUseCase().invoke(season).first()
        standings.sortedBy { it.position }.take(limit).map {
            WidgetDriverStanding(
                pos = it.position,
                code = it.driver.code ?: it.driver.fullName.take(3).uppercase(),
                name = it.driver.fullName,
                teamName = it.team?.name ?: "",
                teamHex = it.team?.colourHex,
                constructorId = it.team?.constructorId,
                points = it.points,
                driverNumber = it.driver.permanentNumber,
            )
        }
    }

    suspend fun loadTopConstructors(context: Context, season: Int, limit: Int = 5): List<WidgetConstructorStanding> = withContext(Dispatchers.IO) {
        val ep = WidgetEntryPointAccess.from(context)
        val standings = ep.getConstructorStandingsUseCase().invoke(season).first()
        standings.sortedBy { it.position }.take(limit).map {
            WidgetConstructorStanding(
                pos = it.position,
                name = it.team.name,
                hex = it.team.colourHex,
                constructorId = it.team.constructorId,
                points = it.points,
            )
        }
    }

    /** Loads ALL drivers for the favorite widget config screen. */
    suspend fun loadAllDrivers(context: Context, season: Int): List<WidgetDriverStanding> = withContext(Dispatchers.IO) {
        val ep = WidgetEntryPointAccess.from(context)
        val standings = ep.getDriverStandingsUseCase().invoke(season).first()
        standings.sortedBy { it.position }.map {
            WidgetDriverStanding(
                pos = it.position,
                code = it.driver.code ?: it.driver.fullName.take(3).uppercase(),
                name = it.driver.fullName,
                teamName = it.team?.name ?: "",
                teamHex = it.team?.colourHex,
                constructorId = it.team?.constructorId,
                points = it.points,
                driverNumber = it.driver.permanentNumber,
            )
        }
    }

    /** Loads ALL constructors for the favorite widget config screen. */
    suspend fun loadAllConstructors(context: Context, season: Int): List<WidgetConstructorStanding> = withContext(Dispatchers.IO) {
        val ep = WidgetEntryPointAccess.from(context)
        val standings = ep.getConstructorStandingsUseCase().invoke(season).first()
        standings.sortedBy { it.position }.map {
            WidgetConstructorStanding(
                pos = it.position,
                name = it.team.name,
                hex = it.team.colourHex,
                constructorId = it.team.constructorId,
                points = it.points,
            )
        }
    }

    /** Finds a single driver by code for the favorite widget. */
    suspend fun findDriver(context: Context, season: Int, code: String): WidgetDriverStanding? =
        loadAllDrivers(context, season).find { it.code == code }

    /** Finds a single constructor by id for the favorite widget. */
    suspend fun findConstructor(context: Context, season: Int, constructorId: String): WidgetConstructorStanding? =
        loadAllConstructors(context, season).find { it.constructorId == constructorId }

    suspend fun refreshAll(
        context: Context,
        scheduleSeason: Int,
        standingsSeason: Int,
    ): Resource<Unit> = WidgetEntryPointAccess.from(context)
        .refreshAllForWidgetUseCase()
        .invoke(scheduleSeason, standingsSeason)

    /** Format a countdown like "2d 5h 12m" or "LIVE". */
    fun formatCountdown(target: Instant?): String {
        if (target == null) return "—"
        val remaining = Duration.between(Instant.now(), target)
        return when {
            remaining.isNegative || remaining.isZero -> "LIVE"
            remaining.toDays() > 0 -> "${remaining.toDays()}d ${remaining.toHours() % 24}h ${remaining.toMinutes() % 60}m"
            remaining.toHours() > 0 -> "${remaining.toHours()}h ${remaining.toMinutes() % 60}m"
            else -> "${remaining.toMinutes()}m"
        }
    }

    /** Resolve the user-selected display zone for the given circuit. */
    suspend fun resolveZone(context: Context, circuitId: String?): java.time.ZoneId {
        val ep = WidgetEntryPointAccess.from(context)
        val settings = ep.settingsDataStore().flow.first()
        return ep.timeZoneResolver().resolve(settings.timeZoneMode, circuitId)
    }

    /** String resource for a session type, for use outside Compose (widgets). */
    private fun sessionLabelRes(type: SessionType?): Int = when (type) {
        SessionType.FP1 -> R.string.session_fp1
        SessionType.FP2 -> R.string.session_fp2
        SessionType.FP3 -> R.string.session_fp3
        SessionType.QUALIFYING -> R.string.session_qualifying
        SessionType.SPRINT -> R.string.session_sprint
        SessionType.SPRINT_QUALIFYING -> R.string.session_sprint_qualifying
        SessionType.RACE -> R.string.session_race
        null -> R.string.session_race
    }

    /**
     * The most recently completed session of the season and its classification — practice,
     * qualifying or race, whichever ran last.
     *
     * Returns null when no session has finished yet, or when OpenF1 has not published the
     * result (the free tier withholds a session until roughly 30 minutes after it ends).
     */
    suspend fun loadLastSession(context: Context, season: Int, limit: Int = 20): WidgetLastSession? =
        withContext(Dispatchers.IO) {
            val ep = WidgetEntryPointAccess.from(context)
            val sessions = ep.scheduleRepository().getSessionsForSeason(season)
                .map { it to it.toDomain() }
                // Unclassifiable sessions are pre-season testing.
                .filter { (_, detail) -> detail.sessionType != null }
            if (sessions.isEmpty()) return@withContext null

            val selection = SessionSelector.select(sessions, Instant.now()) { (_, detail) ->
                SessionWindow(
                    start = detail.dateStart,
                    end = detail.dateEnd ?: detail.dateStart.plusSeconds(
                        SessionSelector.estimatedDurationSeconds(detail.sessionType),
                    ),
                )
            }
            val (entity, detail) = selection.lastFinished ?: return@withContext null

            val results = ep.sessionDetailRepository().getSessionResult(detail.sessionKey)
            if (results.isEmpty()) return@withContext null

            val race = ep.scheduleRepository().getRace(season, entity.round)
            WidgetLastSession(
                sessionLabel = context.getString(sessionLabelRes(detail.sessionType)),
                raceName = race?.raceName ?: detail.circuitShortName,
                rows = results.take(limit).map { r ->
                    WidgetSessionResultRow(
                        pos = r.position,
                        code = r.driverCode,
                        name = r.driverName,
                        teamName = r.teamName,
                        teamHex = r.teamColourHex,
                        timeText = TimeFormatter.formatLapDuration(r.durationSeconds),
                        secondaryText = TimeFormatter.formatGap(r.gapSeconds)
                            ?: r.laps?.let { context.getString(R.string.session_laps_format, it) },
                        statusText = when {
                            r.dsq -> context.getString(R.string.session_dsq)
                            r.dns -> context.getString(R.string.session_dns)
                            r.dnf -> context.getString(R.string.session_dnf)
                            else -> null
                        },
                    )
                },
            )
        }

    /**
     * Format F1 points for display, preserving half-points (e.g. 12.5) without
     * appending a trailing ".0" for whole numbers. Uses a US locale so the decimal
     * separator is always a dot regardless of device locale.
     */
    fun formatPoints(points: Double): String =
        if (points % 1.0 == 0.0) points.toInt().toString()
        else "%.1f".format(java.util.Locale.US, points)

    /**
     * Truncate text for bitmap-rendered labels (Images cannot wrap like Text).
     * Appends an ellipsis when the source exceeds [max] characters.
     */
    fun truncateForBitmap(text: String, max: Int): String =
        if (text.length > max) text.take(max - 1) + "…" else text
}
