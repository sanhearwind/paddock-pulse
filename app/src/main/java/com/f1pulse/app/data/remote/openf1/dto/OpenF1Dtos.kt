package com.f1pulse.app.data.remote.openf1.dto

import com.squareup.moshi.Json

/**
 * OpenF1 API DTOs. OpenF1 returns top-level JSON arrays (no envelope).
 * All fields are nullable or defaulted so Moshi tolerates missing fields.
 * Field names are snake_case in the API; @Json maps them to camelCase properties.
 *
 * Base URL: https://api.openf1.org/v1/
 */

data class OpenF1SessionDto(
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int = 0,
    val location: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "country_name") val countryName: String? = null,
    @Json(name = "circuit_key") val circuitKey: Int? = null,
    @Json(name = "circuit_short_name") val circuitShortName: String? = null,
    // Practice1 / Practice2 / Practice3 / Qualifying / Sprint / SprintQualifying / Race
    @Json(name = "session_type") val sessionType: String? = null,
    @Json(name = "session_name") val sessionName: String? = null,
    @Json(name = "date_start") val dateStart: String? = null,
    @Json(name = "date_end") val dateEnd: String? = null,
    @Json(name = "gmt_offset") val gmtOffset: String? = null,
    val year: Int? = null,
)

data class OpenF1MeetingDto(
    @Json(name = "meeting_key") val meetingKey: Int = 0,
    @Json(name = "circuit_key") val circuitKey: Int? = null,
    @Json(name = "circuit_short_name") val circuitShortName: String? = null,
    val location: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "country_name") val countryName: String? = null,
    val name: String? = null,
    @Json(name = "official_name") val officialName: String? = null,
    val year: Int? = null,
)

data class OpenF1DriverDto(
    @Json(name = "driver_number") val driverNumber: Int = 0,
    @Json(name = "broadcast_name") val broadcastName: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "name_acronym") val nameAcronym: String? = null,
    @Json(name = "team_name") val teamName: String? = null,
    @Json(name = "team_colour") val teamColour: String? = null, // hex without leading #
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "headshot_url") val headshotUrl: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "session_key") val sessionKey: Int? = null,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    val year: Int? = null,
)

data class OpenF1WeatherDto(
    val date: String? = null,
    @Json(name = "session_key") val sessionKey: Int = 0,
    // NOTE: the API field names are `air_temperature` / `track_temperature`, not the
    // abbreviated forms. Getting these wrong silently yields 0°C readings.
    @Json(name = "air_temperature") val airTemp: Double? = null,
    @Json(name = "track_temperature") val trackTemp: Double? = null,
    val humidity: Double? = null,
    val pressure: Double? = null,
    @Json(name = "wind_speed") val windSpeed: Double? = null,
    @Json(name = "wind_direction") val windDirection: Double? = null,
    val rainfall: Double? = null,
)

/**
 * OpenF1 `session_result` — the classification for one session.
 *
 * [duration] and [gapToLeader] are polymorphic: a bare number for practice and race
 * sessions, but a three-element array `[Q1, Q2, Q3]` for qualifying. They are therefore
 * declared as [Any] (Moshi yields `Double` or `List<Any?>`) and normalised in the mapper.
 * Declaring them as `Double?` makes qualifying responses throw `JsonDataException`.
 */
data class OpenF1SessionResultDto(
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    val position: Int? = null,
    @Json(name = "number_of_laps") val numberOfLaps: Int? = null,
    val duration: Any? = null,
    @Json(name = "gap_to_leader") val gapToLeader: Any? = null,
    val dnf: Boolean? = null,
    val dns: Boolean? = null,
    val dsq: Boolean? = null,
    /** Present on race sessions only. */
    val points: Double? = null,
)

data class OpenF1PositionDto(
    val date: String? = null,
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    val position: Int? = null,
    val y: Double? = null,
)

data class OpenF1LapDto(
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    @Json(name = "lap_number") val lapNumber: Int = 0,
    @Json(name = "date_start") val dateStart: String? = null,
    @Json(name = "lap_duration") val lapDuration: Double? = null,
    @Json(name = "is_pit_out_lap") val isPitOutLap: Boolean? = null,
    @Json(name = "lap_start_time") val lapStartTime: String? = null,
    @Json(name = "duration_sector_1") val durationSector1: Double? = null,
    @Json(name = "duration_sector_2") val durationSector2: Double? = null,
    @Json(name = "duration_sector_3") val durationSector3: Double? = null,
)

data class OpenF1PitDto(
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    @Json(name = "lap_number") val lapNumber: Int = 0,
    @Json(name = "pit_out_time") val pitOutTime: String? = null,
    @Json(name = "pit_in_time") val pitInTime: String? = null,
    @Json(name = "pit_duration") val pitDuration: Double? = null,
)

data class OpenF1RaceControlDto(
    val date: String? = null,
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int? = null,
    val lap: Int? = null,
    val category: String? = null,
    val flag: String? = null,
    val scope: String? = null,
    val sector: Int? = null,
    val message: String? = null,
)

data class OpenF1IntervalDto(
    val date: String? = null,
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    @Json(name = "gap_to_leader") val gapToLeader: String? = null,
    @Json(name = "interval_to_position_ahead") val intervalToPositionAhead: String? = null,
    val position: Int? = null,
)

data class OpenF1StintDto(
    @Json(name = "session_key") val sessionKey: Int = 0,
    @Json(name = "meeting_key") val meetingKey: Int? = null,
    @Json(name = "driver_number") val driverNumber: Int = 0,
    @Json(name = "stint_number") val stintNumber: Int = 0,
    @Json(name = "lap_start") val lapStart: Int? = null,
    @Json(name = "lap_end") val lapEnd: Int? = null,
    val compound: String? = null, // SOFT/MEDIUM/HARD/INTERMEDIATE/WET
    @Json(name = "tyre_age_at_start") val tyreAgeAtStart: Int? = null,
)
