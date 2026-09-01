package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.toEntity
import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.StandingDao
import com.f1pulse.app.database.entity.LastUpdatedEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandingsRepository @Inject constructor(
    private val standingDao: StandingDao,
    private val jolpicaApi: JolpicaApi,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val STANDINGS_TTL = 1L * 60 * 60 * 1000 // 1h
    }

    fun observeDriverStandings(season: Int): Flow<List<com.f1pulse.app.database.entity.DriverStandingEntity>> =
        standingDao.observeDriverStandings(season)

    fun observeConstructorStandings(season: Int): Flow<List<com.f1pulse.app.database.entity.ConstructorStandingEntity>> =
        standingDao.observeConstructorStandings(season)

    suspend fun getTopDrivers(season: Int, limit: Int) = standingDao.getTopDrivers(season, limit)

    suspend fun getTopConstructors(season: Int, limit: Int) = standingDao.getTopConstructors(season, limit)

    suspend fun refreshStandings(season: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "standings_$season"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val driversResp = jolpicaApi.getDriverStandings(season.toString())
            val dStandings = driversResp.mrData.standingsTable?.standingsLists
                ?.firstOrNull()?.driverStandings.orEmpty()
            if (dStandings.isNotEmpty()) {
                standingDao.upsertDriverStandings(dStandings.map { it.toEntity(season, now) })
            }

            val ctorResp = jolpicaApi.getConstructorStandings(season.toString())
            val cStandings = ctorResp.mrData.standingsTable?.standingsLists
                ?.firstOrNull()?.constructorStandings.orEmpty()
            if (cStandings.isNotEmpty()) {
                standingDao.upsertConstructorStandings(cStandings.map { it.toEntity(season, now) })
            }

            metaDao.upsert(LastUpdatedEntity(key, now, STANDINGS_TTL))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh standings")
        }
    }
}
