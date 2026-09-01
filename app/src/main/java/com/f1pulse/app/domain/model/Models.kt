package com.f1pulse.app.domain.model

import java.time.Instant

enum class SessionType { FP1, FP2, FP3, QUALIFYING, SPRINT, SPRINT_QUALIFYING, RACE }
enum class TireCompound { SOFT, MEDIUM, HARD, INTERMEDIATE, WET }

/** Circuit metadata loaded from `assets/circuit_meta.json`. */
data class CircuitMeta(
    val svgAsset: String?,
    val timeZone: String?,
    val lengthKm: Double?,
    val corners: Int?,
    val drsZones: Int?,
    val firstHeld: Int?,
    val lapRecord: String?,
)

data class Circuit(
    val circuitId: String,
    val name: String,
    val locality: String,
    val country: String,
    val countryCode: String?,
    val lat: Double,
    val lng: Double,
    val svgAsset: String?,
    val timeZone: String?,
    val lengthKm: Double?,
    val corners: Int?,
    val drsZones: Int?,
    val firstHeld: Int?,
    val lapRecord: String?,
)

data class RaceSessions(
    val fp1: Instant?,
    val fp2: Instant?,
    val fp3: Instant?,
    val qualifying: Instant?,
    val sprint: Instant?,
    val sprintQualifying: Instant?,
    val race: Instant?,
)

data class Race(
    val season: Int,
    val round: Int,
    val raceName: String,
    val circuit: Circuit,
    val sessions: RaceSessions,
    val openF1MeetingKey: Int?,
    val isFinished: Boolean,
)

data class Team(
    val constructorId: String,
    val name: String,
    val colourHex: String?,
    val nationality: String?,
)

data class Driver(
    val driverId: String,
    val code: String?,
    val permanentNumber: Int?,
    val givenName: String,
    val familyName: String,
    val fullName: String,
    val nationality: String,
    val dateOfBirth: String?,
    val team: Team?,
    val headshotUrl: String?,
    val countryCode: String?,
)

data class DriverStanding(
    val position: Int,
    val points: Double,
    val wins: Int,
    val driver: Driver,
    val team: Team?,
)

data class ConstructorStanding(
    val position: Int,
    val points: Double,
    val wins: Int,
    val team: Team,
)

data class RaceResult(
    val position: Int,
    val driver: Driver,
    val team: Team,
    val grid: Int?,
    val laps: Int?,
    val status: String?,
    val raceTimeStr: String?,
    val fastestLapTime: String?,
    val fastestLapRank: Int?,
    val q1: String?,
    val q2: String?,
    val q3: String?,
    val points: Double,
)

data class Weather(
    val airTemp: Double,
    val trackTemp: Double,
    val humidity: Double,
    val pressure: Double,
    val windSpeed: Double,
    val rainfall: Double,
    val date: Instant,
)

data class LapTime(
    val driverNumber: Int,
    val lap: Int,
    val duration: Double?,
    val sectors: List<Double?>,
)

data class PitStop(
    val driverNumber: Int,
    val lap: Int,
    val duration: Double?,
)

data class SessionDetail(
    val sessionKey: Int,
    /** Null when OpenF1 reports a session we cannot classify (e.g. pre-season testing). */
    val sessionType: SessionType?,
    val sessionName: String,
    val dateStart: Instant,
    val dateEnd: Instant?,
    val circuitShortName: String,
    val countryCode: String,
)

/** One driver's classification in a single session, sourced from OpenF1 `session_result`. */
data class SessionResult(
    val position: Int,
    val driverNumber: Int,
    val driverName: String,
    val driverCode: String,
    val teamName: String?,
    val teamColourHex: String?,
    /** Best lap for practice/qualifying; total race time for the race. Seconds. */
    val durationSeconds: Double?,
    /** Gap to the session leader, in seconds. Null for the leader. */
    val gapSeconds: Double?,
    val laps: Int?,
    val dnf: Boolean,
    val dns: Boolean,
    val dsq: Boolean,
    val points: Double?,
)
