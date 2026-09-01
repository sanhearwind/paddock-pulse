package com.f1pulse.app.database.entity

import androidx.room.Entity

/**
 * A single Grand Prix weekend. Composite PK (season, round).
 * Session times are stored as UTC epoch-millis for sortable querying.
 */
@Entity(tableName = "race", primaryKeys = ["season", "round"])
data class RaceEntity(
    val season: Int,
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val circuitName: String,
    val locality: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val raceDateUtc: Long,
    val raceTimeUtc: Long?,
    val fp1Utc: Long?,
    val fp2Utc: Long?,
    val fp3Utc: Long?,
    val qualifyingUtc: Long?,
    val sprintUtc: Long?,
    val sprintQualifyingUtc: Long?,
    val openF1MeetingKey: Int?,
    val circuitSvgAsset: String?,
    val countryFlagCode: String?,
)

/** A single session (FP1/2/3, Qualifying, Sprint, Race) from OpenF1. */
@Entity(tableName = "session", primaryKeys = ["sessionKey"])
data class SessionEntity(
    val sessionKey: Int,
    val meetingKey: Int,
    val season: Int,
    val round: Int,
    val sessionType: String,
    val sessionName: String,
    val dateStartUtc: Long,
    val dateEndUtc: Long?,
    val circuitShortName: String,
    val countryCode: String,
    val countryName: String,
    val location: String,
    val gmtOffset: String?,
)
