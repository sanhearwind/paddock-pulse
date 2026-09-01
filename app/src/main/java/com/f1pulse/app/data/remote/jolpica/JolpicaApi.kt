package com.f1pulse.app.data.remote.jolpica

import com.f1pulse.app.data.remote.jolpica.dto.JolpicaResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Jolpica F1 API — free, no auth, Ergast-compatible.
 * Base URL: https://api.jolpi.ca/ergast/f1/
 */
interface JolpicaApi {

    @GET("current.json")
    suspend fun getCurrentSeason(): JolpicaResponse

    @GET("{season}.json")
    suspend fun getSeason(@Path("season") season: String): JolpicaResponse

    @GET("{season}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("season") season: String,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 30,
    ): JolpicaResponse

    @GET("{season}/{round}/qualifying.json")
    suspend fun getQualifying(
        @Path("season") season: String,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 30,
    ): JolpicaResponse

    @GET("{season}/{round}/sprint.json")
    suspend fun getSprint(
        @Path("season") season: String,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 30,
    ): JolpicaResponse

    @GET("{season}/driverStandings.json")
    suspend fun getDriverStandings(
        @Path("season") season: String,
        @Query("limit") limit: Int = 50,
    ): JolpicaResponse

    @GET("{season}/constructorStandings.json")
    suspend fun getConstructorStandings(
        @Path("season") season: String,
        @Query("limit") limit: Int = 50,
    ): JolpicaResponse

    @GET("{season}/drivers.json")
    suspend fun getDrivers(
        @Path("season") season: String,
        @Query("limit") limit: Int = 50,
    ): JolpicaResponse

    @GET("{season}/constructors.json")
    suspend fun getConstructors(
        @Path("season") season: String,
        @Query("limit") limit: Int = 50,
    ): JolpicaResponse

    @GET("circuits.json")
    suspend fun getCircuits(@Query("limit") limit: Int = 100): JolpicaResponse
}
