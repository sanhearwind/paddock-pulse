package com.f1pulse.app.data.remote.jolpica.dto

import com.squareup.moshi.Json

/**
 * Jolpica / Ergast-compatible DTOs. All responses are wrapped in `MRData`.
 * Moshi ignores unknown fields, so one envelope with nullable tables serves
 * every endpoint; the relevant one is populated per call.
 */
data class JolpicaResponse(
    @Json(name = "MRData") val mrData: JolpicaMrData = JolpicaMrData(),
)

data class JolpicaMrData(
    val season: String? = null,
    val round: String? = null,
    val total: String? = null,
    @Json(name = "RaceTable") val raceTable: RaceTableDto? = null,
    @Json(name = "DriverTable") val driverTable: DriverTableDto? = null,
    @Json(name = "ConstructorTable") val constructorTable: ConstructorTableDto? = null,
    @Json(name = "CircuitTable") val circuitTable: CircuitTableDto? = null,
    @Json(name = "StandingsTable") val standingsTable: StandingsTableDto? = null,
)

// ---- Schedule / Races -------------------------------------------------

data class RaceTableDto(
    val season: String = "",
    val round: String? = null,
    @Json(name = "Races") val races: List<RaceDto> = emptyList(),
)

data class RaceDto(
    val season: String = "",
    val round: String = "",
    val url: String? = null,
    val raceName: String = "",
    val Circuit: CircuitDto = CircuitDto(),
    val date: String = "",
    val time: String? = null,
    val FirstPractice: SessionTimeDto? = null,
    val SecondPractice: SessionTimeDto? = null,
    val ThirdPractice: SessionTimeDto? = null,
    val Qualifying: SessionTimeDto? = null,
    val Sprint: SessionTimeDto? = null,
    val SprintQualifying: SessionTimeDto? = null,
    // populated on results / qualifying / sprint endpoints
    val Results: List<ResultDto>? = null,
    val QualifyingResults: List<QualifyingResultDto>? = null,
    val SprintResults: List<ResultDto>? = null,
)

data class SessionTimeDto(
    val date: String = "",
    val time: String? = null,
)

data class CircuitDto(
    val circuitId: String = "",
    val url: String? = null,
    val circuitName: String = "",
    val Location: LocationDto = LocationDto(),
)

data class LocationDto(
    val lat: String = "0",
    val long: String = "0",
    val locality: String = "",
    val country: String = "",
)

// ---- Drivers / Constructors ------------------------------------------

data class DriverTableDto(
    val season: String = "",
    @Json(name = "Drivers") val drivers: List<DriverDto> = emptyList(),
)

data class ConstructorTableDto(
    val season: String = "",
    @Json(name = "Constructors") val constructors: List<ConstructorDto> = emptyList(),
)

data class CircuitTableDto(
    @Json(name = "Circuits") val circuits: List<CircuitDto> = emptyList(),
)

data class DriverDto(
    val driverId: String = "",
    val permanentNumber: String? = null,
    val code: String? = null,
    val url: String? = null,
    val givenName: String = "",
    val familyName: String = "",
    val dateOfBirth: String? = null,
    val nationality: String = "",
)

data class ConstructorDto(
    val constructorId: String = "",
    val url: String? = null,
    val name: String = "",
    val nationality: String = "",
)

// ---- Results ---------------------------------------------------------

data class ResultDto(
    val number: String = "",
    val position: String = "",
    val positionText: String? = null,
    val points: String = "0",
    val Driver: DriverDto = DriverDto(),
    val Constructor: ConstructorDto = ConstructorDto(),
    val grid: String? = null,
    val laps: String? = null,
    val status: String? = null,
    val Time: ResultTimeDto? = null,
    val FastestLap: FastestLapDto? = null,
)

data class ResultTimeDto(
    val millis: String? = null,
    val time: String? = null,
)

data class FastestLapDto(
    val rank: String? = null,
    val lap: String? = null,
    val Time: LapTimeDto? = null,
    val AverageSpeed: AverageSpeedDto? = null,
)

data class LapTimeDto(val time: String? = null)

data class AverageSpeedDto(val units: String? = null, val speed: String? = null)

data class QualifyingResultDto(
    val number: String = "",
    val position: String = "",
    val Driver: DriverDto = DriverDto(),
    val Constructor: ConstructorDto = ConstructorDto(),
    val Q1: String? = null,
    val Q2: String? = null,
    val Q3: String? = null,
)

// ---- Standings -------------------------------------------------------

data class StandingsTableDto(
    val season: String = "",
    @Json(name = "StandingsLists") val standingsLists: List<StandingsListDto> = emptyList(),
)

data class StandingsListDto(
    val season: String = "",
    val round: String = "",
    @Json(name = "DriverStandings") val driverStandings: List<DriverStandingDto>? = null,
    @Json(name = "ConstructorStandings") val constructorStandings: List<ConstructorStandingDto>? = null,
)

data class DriverStandingDto(
    val position: String = "",
    val positionText: String? = null,
    val points: String = "0",
    val wins: String = "0",
    val Driver: DriverDto = DriverDto(),
    @Json(name = "Constructors") val constructors: List<ConstructorDto> = emptyList(),
)

data class ConstructorStandingDto(
    val position: String = "",
    val positionText: String? = null,
    val points: String = "0",
    val wins: String = "0",
    val Constructor: ConstructorDto = ConstructorDto(),
)
