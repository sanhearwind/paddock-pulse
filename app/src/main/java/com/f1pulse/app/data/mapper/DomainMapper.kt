package com.f1pulse.app.data.mapper

import com.f1pulse.app.core.time.RaceTiming
import com.f1pulse.app.database.entity.ConstructorStandingEntity
import com.f1pulse.app.database.entity.DriverEntity
import com.f1pulse.app.database.entity.DriverStandingEntity
import com.f1pulse.app.database.entity.RaceEntity
import com.f1pulse.app.database.entity.ResultEntity
import com.f1pulse.app.database.entity.SessionEntity
import com.f1pulse.app.database.entity.SessionResultEntity
import com.f1pulse.app.database.entity.WeatherEntity
import com.f1pulse.app.domain.model.Circuit
import com.f1pulse.app.domain.model.CircuitMeta
import com.f1pulse.app.domain.model.ConstructorStanding
import com.f1pulse.app.domain.model.Driver
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.domain.model.RaceResult
import com.f1pulse.app.domain.model.RaceSessions
import com.f1pulse.app.domain.model.SessionDetail
import com.f1pulse.app.domain.model.SessionResult
import com.f1pulse.app.domain.model.SessionType
import com.f1pulse.app.domain.model.Team
import com.f1pulse.app.domain.model.Weather
import java.time.Instant

private fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
private fun Long?.toInstantOrNull(): Instant? = this?.let { Instant.ofEpochMilli(it) }

fun RaceEntity.toCircuit(meta: CircuitMeta? = null): Circuit = Circuit(
    circuitId = circuitId,
    name = circuitName,
    locality = locality,
    country = country,
    countryCode = countryFlagCode,
    lat = lat,
    lng = lng,
    svgAsset = meta?.svgAsset ?: circuitSvgAsset,
    timeZone = meta?.timeZone,
    lengthKm = meta?.lengthKm,
    corners = meta?.corners,
    drsZones = meta?.drsZones,
    firstHeld = meta?.firstHeld,
    lapRecord = meta?.lapRecord,
)

fun RaceEntity.toDomain(
    sessions: List<SessionEntity>,
    circuitMeta: CircuitMeta? = null,
    now: Long,
): Race = Race(
    season = season,
    round = round,
    raceName = raceName,
    circuit = toCircuit(circuitMeta),
    sessions = RaceSessions(
        fp1 = fp1Utc.toInstantOrNull(),
        fp2 = fp2Utc.toInstantOrNull(),
        fp3 = fp3Utc.toInstantOrNull(),
        qualifying = qualifyingUtc.toInstantOrNull(),
        sprint = sprintUtc.toInstantOrNull(),
        sprintQualifying = sprintQualifyingUtc.toInstantOrNull(),
        race = raceDateUtc.toInstantOrNull(),
    ),
    openF1MeetingKey = openF1MeetingKey,
    isFinished = RaceTiming.isFinished(raceDateUtc, now),
)

fun DriverEntity.toDomain(): Driver = Driver(
    driverId = driverId,
    code = code,
    permanentNumber = permanentNumber,
    givenName = givenName,
    familyName = familyName,
    fullName = fullName,
    nationality = nationality,
    dateOfBirth = dateOfBirth,
    team = teamName?.let { Team(constructorId = "", name = it, colourHex = teamColourHex, nationality = null) },
    headshotUrl = headshotUrl,
    countryCode = countryCode,
)

private fun buildTeam(constructorId: String?, name: String?, colourHex: String?): Team? =
    name?.let { Team(constructorId ?: "", it, colourHex, null) }

fun DriverStandingEntity.toDomain(): DriverStanding {
    val team = buildTeam(constructorId, constructorName, teamColourHex)
    return DriverStanding(
        position = position,
        points = points,
        wins = wins,
        driver = Driver(
            driverId = driverId,
            code = driverCode,
            permanentNumber = null,
            givenName = driverName.substringBefore(' '),
            familyName = driverName.substringAfter(' ', ""),
            fullName = driverName,
            nationality = "",
            dateOfBirth = null,
            team = team,
            headshotUrl = null,
            countryCode = null,
        ),
        team = team,
    )
}

fun ConstructorStandingEntity.toDomain(): ConstructorStanding = ConstructorStanding(
    position = position,
    points = points,
    wins = wins,
    team = Team(constructorId, constructorName, teamColourHex, null),
)

fun ResultEntity.toDomain(): RaceResult {
    val team = Team(constructorId, constructorName, teamColourHex, null)
    return RaceResult(
        position = position,
        driver = Driver(
            driverId = driverId,
            code = driverCode,
            permanentNumber = null,
            givenName = driverName.substringBefore(' '),
            familyName = driverName.substringAfter(' ', ""),
            fullName = driverName,
            nationality = "",
            dateOfBirth = null,
            team = team,
            headshotUrl = null,
            countryCode = null,
        ),
        team = team,
        grid = grid,
        laps = laps,
        status = status,
        raceTimeStr = raceTimeStr,
        fastestLapTime = fastestLapTime,
        fastestLapRank = fastestLapRank,
        q1 = q1,
        q2 = q2,
        q3 = q3,
        points = points,
    )
}

fun WeatherEntity.toDomain(): Weather = Weather(
    airTemp = airTemp,
    trackTemp = trackTemp,
    humidity = humidity,
    pressure = pressure,
    windSpeed = windSpeed,
    rainfall = rainfall,
    date = dateUtc.toInstant(),
)

fun SessionEntity.toDomain(): SessionDetail = SessionDetail(
    sessionKey = sessionKey,
    sessionType = mapSessionType(sessionType, sessionName),
    sessionName = sessionName,
    dateStart = dateStartUtc.toInstant(),
    dateEnd = dateEndUtc.toInstantOrNull(),
    circuitShortName = circuitShortName,
    countryCode = countryCode,
)

fun SessionResultEntity.toDomain(): SessionResult = SessionResult(
    position = position,
    driverNumber = driverNumber,
    driverName = driverName,
    driverCode = driverCode,
    teamName = teamName,
    teamColourHex = teamColourHex,
    durationSeconds = durationSeconds,
    gapSeconds = gapSeconds,
    laps = laps,
    dnf = dnf,
    dns = dns,
    dsq = dsq,
    points = points,
)

/**
 * Resolves an OpenF1 session to a [SessionType].
 *
 * OpenF1's `session_type` is coarse — only `Practice`, `Qualifying` and `Race` — so the
 * finer distinctions (FP1 vs FP3, Sprint vs Race) only exist in `session_name`. Both are
 * therefore required.
 *
 * Returns `null` for anything unrecognised (notably pre-season testing, whose sessions are
 * named `Day 1`..`Day 3`). Callers must drop unknown sessions rather than guess: an earlier
 * `else -> RACE` fallback silently classified every practice session as the race, which
 * made the weather lookup read a different session key than the one it wrote.
 */
fun mapSessionType(rawType: String, rawName: String): SessionType? {
    val name = rawName.trim().lowercase().replace(" ", "")
    // `session_name` alone is unambiguous when present; fall back to `session_type`.
    return when (name) {
        "practice1" -> SessionType.FP1
        "practice2" -> SessionType.FP2
        "practice3" -> SessionType.FP3
        "sprintqualifying", "sprintshootout" -> SessionType.SPRINT_QUALIFYING
        "sprint" -> SessionType.SPRINT
        "qualifying" -> SessionType.QUALIFYING
        "race" -> SessionType.RACE
        else -> when (rawType.trim().lowercase()) {
            // A qualifying/race session we cannot name precisely is still usable;
            // an unnamed practice session is not (we could not tell FP1 from FP3).
            "qualifying" -> SessionType.QUALIFYING
            "race" -> SessionType.RACE
            else -> null
        }
    }
}
