package com.f1pulse.app.database.entity

import androidx.room.Entity

/**
 * Results for a session within a race weekend.
 * sessionType: "RACE" | "SPRINT" | "QUALIFYING"
 */
@Entity(
    tableName = "result",
    primaryKeys = ["season", "round", "sessionType", "driverId"]
)
data class ResultEntity(
    val season: Int,
    val round: Int,
    val sessionType: String,
    val position: Int,
    val driverId: String,
    val driverName: String,
    val driverCode: String?,
    val constructorId: String,
    val constructorName: String,
    val teamColourHex: String?,
    val grid: Int?,
    val laps: Int?,
    val status: String?,
    val raceTimeStr: String?,
    val raceTimeMillis: Long?,
    val fastestLapTime: String?,
    val fastestLapRank: Int?,
    val q1: String?,
    val q2: String?,
    val q3: String?,
    val points: Double,
)

@Entity(tableName = "weather", primaryKeys = ["sessionKey", "dateUtc"])
data class WeatherEntity(
    val sessionKey: Int,
    val dateUtc: Long,
    val airTemp: Double,
    val trackTemp: Double,
    val humidity: Double,
    val pressure: Double,
    val windSpeed: Double,
    val rainfall: Double,
)

/**
 * One driver's classification in a single OpenF1 session, including practice — which
 * Jolpica/Ergast does not publish at all.
 *
 * Keyed by OpenF1's globally unique `session_key` plus the car number, so practice,
 * qualifying and race classifications for the same weekend coexist without collision.
 */
@Entity(tableName = "session_result", primaryKeys = ["sessionKey", "driverNumber"])
data class SessionResultEntity(
    val sessionKey: Int,
    val driverNumber: Int,
    val position: Int,
    // Denormalised from OpenF1's per-session driver list, mirroring how ResultEntity stores
    // driver/team fields. Practice sessions frequently field reserve drivers who never appear
    // in the season-level driver table, so a join against it would leave them unnamed.
    val driverName: String,
    val driverCode: String,
    val teamName: String?,
    val teamColourHex: String?,
    /** Best lap for practice/qualifying, total race time for the race. Seconds. */
    val durationSeconds: Double?,
    /** Gap to the session leader in seconds; null for the leader or when unavailable. */
    val gapSeconds: Double?,
    val laps: Int?,
    val dnf: Boolean,
    val dns: Boolean,
    val dsq: Boolean,
    /** Race sessions only. */
    val points: Double?,
)
