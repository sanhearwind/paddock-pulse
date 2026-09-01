package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.toEntity
import com.f1pulse.app.data.remote.openf1.OpenF1Api
import com.f1pulse.app.data.remote.openf1.dto.OpenF1IntervalDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1RaceControlDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1StintDto
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.SessionDao
import com.f1pulse.app.database.dao.SessionResultDao
import com.f1pulse.app.database.dao.WeatherDao
import com.f1pulse.app.database.entity.LastUpdatedEntity
import com.f1pulse.app.database.entity.SessionEntity
import com.f1pulse.app.database.entity.SessionResultEntity
import com.f1pulse.app.database.entity.WeatherEntity
import com.f1pulse.app.domain.model.LapTime
import com.f1pulse.app.domain.model.PitStop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-level detail (weather, laps, pit stops, intervals, stints, race control) from OpenF1.
 * Weather is cached; the rest are pulled on demand (no caching) since they can be large.
 */
@Singleton
class SessionDetailRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val weatherDao: WeatherDao,
    private val sessionResultDao: SessionResultDao,
    private val openF1Api: OpenF1Api,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val WEATHER_TTL = 5L * 60 * 1000 // 5min
        /** Results are final once published, so they can be cached hard. */
        private val SESSION_RESULT_TTL = 24L * 60 * 60 * 1000 // 24h
        /**
         * Retry cadence while a session has no published result yet. On the free OpenF1 tier a
         * session stays unavailable until ~30min after it ends, so we re-check periodically
         * rather than caching the empty answer.
         */
        private val SESSION_RESULT_PENDING_TTL = 2L * 60 * 1000 // 2min
    }

    fun observeWeather(sessionKey: Int): Flow<WeatherEntity?> = weatherDao.observeLatest(sessionKey)

    fun observeSessionResult(sessionKey: Int): Flow<List<SessionResultEntity>> =
        sessionResultDao.observeForSession(sessionKey)

    suspend fun getSessionResult(sessionKey: Int): List<SessionResultEntity> =
        sessionResultDao.getForSession(sessionKey)

    /**
     * Pulls the classification for one session.
     *
     * OpenF1 answers **404** — not an empty array — for a session that has not produced a
     * result yet (not started, or still inside the paid-only live window). That is a normal
     * state, not a failure, so it resolves to [Resource.Success] with nothing stored and a
     * short TTL so the next check happens soon.
     */
    suspend fun refreshSessionResult(sessionKey: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "session_result_$sessionKey"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val results = openF1Api.getSessionResult(mapOf("session_key" to sessionKey.toString()))
            // Driver identity comes from the per-session list rather than the season table:
            // practice sessions routinely field reserve drivers absent from the latter.
            val driversByNumber = if (results.isEmpty()) {
                emptyMap()
            } else {
                runCatching {
                    openF1Api.getDrivers(mapOf("session_key" to sessionKey.toString()))
                        .associateBy { it.driverNumber }
                }.getOrDefault(emptyMap())
            }
            val rows = results.mapNotNull { it.toEntity(driversByNumber[it.driverNumber]) }
            if (rows.isNotEmpty()) sessionResultDao.upsertAll(rows)
            val ttl = if (rows.isEmpty()) SESSION_RESULT_PENDING_TTL else SESSION_RESULT_TTL
            metaDao.upsert(LastUpdatedEntity(key, now, ttl))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            if (http.code() == 404) {
                // No result published yet — expected before/during a session.
                metaDao.upsert(LastUpdatedEntity(key, now, SESSION_RESULT_PENDING_TTL))
                Resource.Success(Unit)
            } else {
                Resource.Error(http.message() ?: "Failed to refresh session result")
            }
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh session result")
        }
    }

    suspend fun getLatestWeather(sessionKey: Int): WeatherEntity? = weatherDao.getLatest(sessionKey)

    suspend fun getSessionsForMeeting(meetingKey: Int): List<SessionEntity> =
        sessionDao.getForMeeting(meetingKey)

    suspend fun getSessionByKey(key: Int): SessionEntity? = sessionDao.getByKey(key)

    suspend fun refreshWeather(sessionKey: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "weather_$sessionKey"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val list = openF1Api.getWeather(mapOf("session_key" to sessionKey.toString()))
            if (list.isNotEmpty()) weatherDao.upsertAll(list.map { it.toEntity() })
            metaDao.upsert(LastUpdatedEntity(key, now, WEATHER_TTL))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh weather")
        }
    }

    suspend fun getLaps(sessionKey: Int, driverNumber: Int? = null): List<LapTime> = withContext(io) {
        val filters = mutableMapOf("session_key" to sessionKey.toString())
        driverNumber?.let { filters["driver_number"] = it.toString() }
        openF1Api.getLaps(filters).map {
            LapTime(
                driverNumber = it.driverNumber,
                lap = it.lapNumber,
                duration = it.lapDuration,
                sectors = listOf(it.durationSector1, it.durationSector2, it.durationSector3),
            )
        }
    }

    suspend fun getPitStops(sessionKey: Int): List<PitStop> = withContext(io) {
        openF1Api.getPitStops(mapOf("session_key" to sessionKey.toString()))
            .map { PitStop(it.driverNumber, it.lapNumber, it.pitDuration) }
    }

    suspend fun getIntervals(sessionKey: Int): List<OpenF1IntervalDto> = withContext(io) {
        openF1Api.getIntervals(mapOf("session_key" to sessionKey.toString()))
    }

    suspend fun getStints(sessionKey: Int): List<OpenF1StintDto> = withContext(io) {
        openF1Api.getStints(mapOf("session_key" to sessionKey.toString()))
    }

    suspend fun getRaceControl(sessionKey: Int): List<OpenF1RaceControlDto> = withContext(io) {
        openF1Api.getRaceControl(mapOf("session_key" to sessionKey.toString()))
    }
}
