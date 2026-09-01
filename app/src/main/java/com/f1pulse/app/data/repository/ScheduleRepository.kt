package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.mapSessionType
import com.f1pulse.app.data.mapper.toEntity
import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.data.remote.openf1.OpenF1Api
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.RaceDao
import com.f1pulse.app.database.dao.SessionDao
import com.f1pulse.app.database.entity.LastUpdatedEntity
import com.f1pulse.app.database.entity.RaceEntity
import com.f1pulse.app.database.entity.SessionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-bound-resource repository for the race schedule. Jolpica is the authoritative
 * source for the calendar (race + session times); OpenF1 enriches with `meeting_key` and
 * precise session `date_start`/`date_end`. OpenF1 failure is non-fatal.
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val raceDao: RaceDao,
    private val sessionDao: SessionDao,
    private val jolpicaApi: JolpicaApi,
    private val openF1Api: OpenF1Api,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val SCHEDULE_TTL = 6L * 60 * 60 * 1000 // 6h
    }

    fun observeNextRace(season: Int, now: Long): Flow<RaceEntity?> =
        raceDao.observeNextRace(season, now)

    fun observeLastRace(season: Int, now: Long): Flow<RaceEntity?> =
        raceDao.observeLastRace(season, now)

    fun observeSchedule(season: Int): Flow<List<RaceEntity>> = raceDao.observeAll(season)

    fun observeRace(season: Int, round: Int): Flow<RaceEntity?> = raceDao.observeRace(season, round)

    fun observeSessions(season: Int, round: Int): Flow<List<SessionEntity>> =
        sessionDao.observeForRound(season, round)

    /** Every session of a season, chronological. Used by widgets to find the last one run. */
    suspend fun getSessionsForSeason(season: Int): List<SessionEntity> =
        sessionDao.getForSeason(season)

    suspend fun getRace(season: Int, round: Int): RaceEntity? = raceDao.getRace(season, round)

    suspend fun getRaceByMeetingKey(season: Int, meetingKey: Int): RaceEntity? =
        raceDao.getRaceByMeetingKey(season, meetingKey)

    suspend fun refreshSchedule(season: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "schedule_$season"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            // 1. Jolpica schedule (authoritative).
            val response = jolpicaApi.getSeason(season.toString())
            val races = response.mrData.raceTable?.races.orEmpty().mapNotNull { it.toEntity(season) }
            if (races.isEmpty()) {
                return@withContext Resource.Error("Schedule response was empty")
            }
            raceDao.upsertAll(races)

            // 2. OpenF1 meetings + sessions (enrichment; failure non-fatal).
            try {
                coroutineScope {
                    val meetingsDef = async { openF1Api.getMeetings(mapOf("year" to season.toString())) }
                    val sessionsDef = async { openF1Api.getSessions(mapOf("year" to season.toString())) }
                    val meetings = meetingsDef.await()
                    val sessions = sessionsDef.await()

                    val matcher = RaceMatcher(races)

                    val keyed = meetings.mapNotNull { meeting ->
                        val match = matcher.match(meeting.countryName, meeting.location) ?: return@mapNotNull null
                        match.copy(openF1MeetingKey = meeting.meetingKey)
                    }
                    if (keyed.isNotEmpty()) raceDao.upsertAll(keyed)

                    val sessionEntities = sessions.mapNotNull { dto ->
                        // An unclassifiable session is pre-season testing (named "Day 1".."Day 3")
                        // or a format we do not model; either way it does not belong to a round.
                        if (mapSessionType(dto.sessionType.orEmpty(), dto.sessionName.orEmpty()) == null) {
                            return@mapNotNull null
                        }
                        val race = matcher.match(dto.countryName, dto.location) ?: return@mapNotNull null
                        dto.toEntity(season, race.round)
                    }
                    if (sessionEntities.isNotEmpty()) sessionDao.upsertAll(sessionEntities)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // OpenF1 enrichment failure must not block schedule refresh.
            }

            metaDao.upsert(LastUpdatedEntity(key, now, SCHEDULE_TTL))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh schedule")
        }
    }
}
