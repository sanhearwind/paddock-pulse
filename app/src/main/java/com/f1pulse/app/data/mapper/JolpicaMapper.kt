package com.f1pulse.app.data.mapper

import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.TeamColors
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.remote.jolpica.dto.ConstructorDto
import com.f1pulse.app.data.remote.jolpica.dto.ConstructorStandingDto
import com.f1pulse.app.data.remote.jolpica.dto.DriverDto
import com.f1pulse.app.data.remote.jolpica.dto.DriverStandingDto
import com.f1pulse.app.data.remote.jolpica.dto.QualifyingResultDto
import com.f1pulse.app.data.remote.jolpica.dto.RaceDto
import com.f1pulse.app.data.remote.jolpica.dto.ResultDto
import com.f1pulse.app.database.entity.ConstructorEntity
import com.f1pulse.app.database.entity.ConstructorStandingEntity
import com.f1pulse.app.database.entity.DriverEntity
import com.f1pulse.app.database.entity.DriverStandingEntity
import com.f1pulse.app.database.entity.RaceEntity
import com.f1pulse.app.database.entity.ResultEntity

// ---- Jolpica DTO → Room Entity --------------------------------------------

fun RaceDto.toEntity(season: Int): RaceEntity? {
    val epoch = TimeFormatter.parseJolpica(date, time)?.toEpochMilli() ?: return null
    return RaceEntity(
        season = season,
        round = round.toIntOrNull() ?: 0,
        raceName = raceName,
        circuitId = Circuit.circuitId,
        circuitName = Circuit.circuitName,
        locality = Circuit.Location.locality,
        country = Circuit.Location.country,
        lat = Circuit.Location.lat.toDoubleOrNull() ?: 0.0,
        lng = Circuit.Location.long.toDoubleOrNull() ?: 0.0,
        raceDateUtc = epoch,
        raceTimeUtc = epoch,
        fp1Utc = FirstPractice?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        fp2Utc = SecondPractice?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        fp3Utc = ThirdPractice?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        qualifyingUtc = Qualifying?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        sprintUtc = Sprint?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        sprintQualifyingUtc = SprintQualifying?.let { TimeFormatter.parseJolpica(it.date, it.time)?.toEpochMilli() },
        openF1MeetingKey = null,
        circuitSvgAsset = "tracks/${Circuit.circuitId}.webp",
        countryFlagCode = CountryFlags.fromLocation(Circuit.Location.country),
    )
}

fun DriverDto.toEntity(
    season: Int,
    teamName: String? = null,
    teamColourHex: String? = null,
    headshotUrl: String? = null,
    countryCode: String? = null,
): DriverEntity = DriverEntity(
    season = season,
    driverId = driverId,
    permanentNumber = permanentNumber?.toIntOrNull(),
    code = code,
    givenName = givenName,
    familyName = familyName,
    fullName = "$givenName $familyName".trim(),
    nationality = nationality,
    dateOfBirth = dateOfBirth,
    teamName = teamName,
    teamColourHex = teamColourHex,
    headshotUrl = headshotUrl,
    countryCode = countryCode,
)

fun ConstructorDto.toEntity(season: Int, colourHex: String? = null): ConstructorEntity =
    ConstructorEntity(
        season = season,
        constructorId = constructorId,
        name = name,
        nationality = nationality,
        colourHex = colourHex ?: TeamColors.hexFor(constructorId),
    )

fun DriverStandingDto.toEntity(
    season: Int,
    lastUpdated: Long,
    teamColourHex: String? = null,
): DriverStandingEntity {
    val ctor = constructors.firstOrNull()
    return DriverStandingEntity(
        season = season,
        position = position.toIntOrNull() ?: 0,
        points = points.toDoubleOrNull() ?: 0.0,
        wins = wins.toIntOrNull() ?: 0,
        driverId = Driver.driverId,
        constructorId = ctor?.constructorId,
        driverName = "${Driver.givenName} ${Driver.familyName}".trim(),
        driverCode = Driver.code,
        constructorName = ctor?.name,
        teamColourHex = teamColourHex ?: ctor?.let { TeamColors.hexFor(it.constructorId) },
        lastUpdated = lastUpdated,
    )
}

fun ConstructorStandingDto.toEntity(
    season: Int,
    lastUpdated: Long,
    colourHex: String? = null,
): ConstructorStandingEntity = ConstructorStandingEntity(
    season = season,
    position = position.toIntOrNull() ?: 0,
    points = points.toDoubleOrNull() ?: 0.0,
    wins = wins.toIntOrNull() ?: 0,
    constructorId = Constructor.constructorId,
    constructorName = Constructor.name,
    teamColourHex = colourHex ?: TeamColors.hexFor(Constructor.constructorId),
    lastUpdated = lastUpdated,
)

fun ResultDto.toResultEntity(season: Int, round: Int, sessionType: String): ResultEntity =
    ResultEntity(
        season = season,
        round = round,
        sessionType = sessionType,
        position = position.toIntOrNull() ?: 0,
        driverId = Driver.driverId,
        driverName = "${Driver.givenName} ${Driver.familyName}".trim(),
        driverCode = Driver.code,
        constructorId = Constructor.constructorId,
        constructorName = Constructor.name,
        teamColourHex = TeamColors.hexFor(Constructor.constructorId),
        grid = grid?.toIntOrNull(),
        laps = laps?.toIntOrNull(),
        status = status,
        raceTimeStr = Time?.time,
        raceTimeMillis = Time?.millis?.toLongOrNull(),
        fastestLapTime = FastestLap?.Time?.time,
        fastestLapRank = FastestLap?.rank?.toIntOrNull(),
        q1 = null,
        q2 = null,
        q3 = null,
        points = points.toDoubleOrNull() ?: 0.0,
    )

fun QualifyingResultDto.toResultEntity(season: Int, round: Int): ResultEntity = ResultEntity(
    season = season,
    round = round,
    sessionType = "QUALIFYING",
    position = position.toIntOrNull() ?: 0,
    driverId = Driver.driverId,
    driverName = "${Driver.givenName} ${Driver.familyName}".trim(),
    driverCode = Driver.code,
    constructorId = Constructor.constructorId,
    constructorName = Constructor.name,
    teamColourHex = TeamColors.hexFor(Constructor.constructorId),
    grid = null,
    laps = null,
    status = null,
    raceTimeStr = null,
    raceTimeMillis = null,
    fastestLapTime = null,
    fastestLapRank = null,
    q1 = Q1,
    q2 = Q2,
    q3 = Q3,
    points = 0.0,
)
