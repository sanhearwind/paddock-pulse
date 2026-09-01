package com.f1pulse.app.data.mapper

import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.remote.openf1.dto.OpenF1DriverDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1SessionDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1SessionResultDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1WeatherDto
import com.f1pulse.app.database.entity.SessionEntity
import com.f1pulse.app.database.entity.SessionResultEntity
import com.f1pulse.app.database.entity.WeatherEntity

// ---- OpenF1 DTO → Room Entity ---------------------------------------------

fun OpenF1SessionDto.toEntity(season: Int, round: Int): SessionEntity = SessionEntity(
    sessionKey = sessionKey,
    meetingKey = meetingKey,
    season = season,
    round = round,
    sessionType = sessionType ?: "",
    sessionName = sessionName ?: "",
    dateStartUtc = TimeFormatter.parseIso(dateStart)?.toEpochMilli() ?: 0L,
    dateEndUtc = TimeFormatter.parseIso(dateEnd)?.toEpochMilli(),
    circuitShortName = circuitShortName ?: "",
    countryCode = countryCode ?: "",
    countryName = countryName ?: "",
    location = location ?: "",
    gmtOffset = gmtOffset,
)

fun OpenF1WeatherDto.toEntity(): WeatherEntity = WeatherEntity(
    sessionKey = sessionKey,
    dateUtc = TimeFormatter.parseIso(date)?.toEpochMilli() ?: 0L,
    airTemp = airTemp ?: 0.0,
    trackTemp = trackTemp ?: 0.0,
    humidity = humidity ?: 0.0,
    pressure = pressure ?: 0.0,
    windSpeed = windSpeed ?: 0.0,
    rainfall = rainfall ?: 0.0,
)

/**
 * Collapses OpenF1's polymorphic `duration` / `gap_to_leader` into a single value.
 *
 * Practice and race sessions send a bare number. Qualifying sends `[Q1, Q2, Q3]`, with
 * `null` for segments the driver did not reach — so the driver's best effort is the *last
 * non-null* entry (Q3 if they got there, otherwise Q2, otherwise Q1).
 */
internal fun Any?.toSessionSeconds(): Double? = when (this) {
    is Number -> toDouble()
    is List<*> -> filterIsInstance<Number>().lastOrNull()?.toDouble()
    else -> null
}

fun OpenF1SessionResultDto.toEntity(driver: OpenF1DriverDto?): SessionResultEntity? {
    val pos = position ?: return null
    return SessionResultEntity(
        sessionKey = sessionKey,
        driverNumber = driverNumber,
        position = pos,
        driverName = driver?.fullName ?: driver?.broadcastName ?: "#$driverNumber",
        driverCode = driver?.nameAcronym ?: driverNumber.toString(),
        teamName = driver?.teamName,
        teamColourHex = driver?.teamColour,
        durationSeconds = duration.toSessionSeconds(),
        gapSeconds = gapToLeader.toSessionSeconds(),
        laps = numberOfLaps,
        dnf = dnf ?: false,
        dns = dns ?: false,
        dsq = dsq ?: false,
        points = points,
    )
}

/**
 * Pairing of OpenF1 driver number with enriched team info, used by DriversRepository
 * to merge team colour / headshot into a Jolpica-sourced [com.f1pulse.app.database.entity.DriverEntity].
 */
data class DriverTeamInfo(
    val driverNumber: Int,
    val teamName: String?,
    val teamColourHex: String?,
    val headshotUrl: String?,
    val countryCode: String?,
)

fun OpenF1DriverDto.toTeamInfo(): DriverTeamInfo = DriverTeamInfo(
    driverNumber = driverNumber,
    teamName = teamName,
    teamColourHex = teamColour,
    headshotUrl = headshotUrl,
    countryCode = countryCode,
)
