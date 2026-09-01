package com.f1pulse.app.data.remote.openf1

import com.f1pulse.app.data.remote.openf1.dto.OpenF1DriverDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1IntervalDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1LapDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1MeetingDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1PitDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1PositionDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1RaceControlDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1SessionDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1SessionResultDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1StintDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1WeatherDto
import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * OpenF1 API — free, no auth, top-level JSON arrays.
 * Base URL: https://api.openf1.org/v1/
 *
 * OpenF1 supports filter operators in the URL (e.g. `?speed>=315`), which Retrofit's
 * @Query cannot express. Read-heavy endpoints therefore take a [QueryMap] so callers
 * can build filters (including operators) as Map<String, String>.
 */
interface OpenF1Api {

    @GET("sessions")
    suspend fun getSessions(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1SessionDto>

    /**
     * Final classification for a session. Note: OpenF1 answers 404 (not an empty array) for
     * sessions that have not produced a result yet — see `SessionDetailRepository` for how
     * that is normalised into "no data yet".
     */
    @GET("session_result")
    suspend fun getSessionResult(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1SessionResultDto>

    @GET("meetings")
    suspend fun getMeetings(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1MeetingDto>

    @GET("drivers")
    suspend fun getDrivers(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1DriverDto>

    @GET("weather")
    suspend fun getWeather(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1WeatherDto>

    @GET("position")
    suspend fun getPositions(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1PositionDto>

    @GET("laps")
    suspend fun getLaps(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1LapDto>

    @GET("pit")
    suspend fun getPitStops(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1PitDto>

    @GET("race_control")
    suspend fun getRaceControl(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1RaceControlDto>

    @GET("interval")
    suspend fun getIntervals(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1IntervalDto>

    @GET("stints")
    suspend fun getStints(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<OpenF1StintDto>
}
