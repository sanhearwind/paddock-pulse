package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.toResultEntity
import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.ResultDao
import com.f1pulse.app.database.entity.LastUpdatedEntity
import com.f1pulse.app.database.entity.ResultEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultsRepository @Inject constructor(
    private val resultDao: ResultDao,
    private val jolpicaApi: JolpicaApi,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val RESULT_TTL = 30L * 60 * 1000 // 30min
    }

    fun observeResults(season: Int, round: Int, sessionType: String): Flow<List<ResultEntity>> =
        resultDao.observeResults(season, round, sessionType)

    /** Refreshes RACE + QUALIFYING + SPRINT for the round (each skipped if still fresh). */
    suspend fun refreshResults(season: Int, round: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val now = TimeFormatter.now().toEpochMilli()
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val raceKey = "result_${season}_${round}_RACE"
            val qualKey = "result_${season}_${round}_QUALIFYING"
            val sprintKey = "result_${season}_${round}_SPRINT"

            if (force || metaDao.isFresh(raceKey, now) != 1) {
                val resp = jolpicaApi.getRaceResults(season.toString(), round)
                val results = resp.mrData.raceTable?.races?.firstOrNull()?.Results.orEmpty()
                if (results.isNotEmpty()) {
                    resultDao.upsertAll(results.map { it.toResultEntity(season, round, "RACE") })
                }
                metaDao.upsert(LastUpdatedEntity(raceKey, now, RESULT_TTL))
            }
            if (force || metaDao.isFresh(qualKey, now) != 1) {
                val resp = jolpicaApi.getQualifying(season.toString(), round)
                val q = resp.mrData.raceTable?.races?.firstOrNull()?.QualifyingResults.orEmpty()
                if (q.isNotEmpty()) {
                    resultDao.upsertAll(q.map { it.toResultEntity(season, round) })
                }
                metaDao.upsert(LastUpdatedEntity(qualKey, now, RESULT_TTL))
            }
            if (force || metaDao.isFresh(sprintKey, now) != 1) {
                val resp = jolpicaApi.getSprint(season.toString(), round)
                val s = resp.mrData.raceTable?.races?.firstOrNull()?.SprintResults.orEmpty()
                if (s.isNotEmpty()) {
                    resultDao.upsertAll(s.map { it.toResultEntity(season, round, "SPRINT") })
                }
                metaDao.upsert(LastUpdatedEntity(sprintKey, now, RESULT_TTL))
            }
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh results")
        }
    }
}
