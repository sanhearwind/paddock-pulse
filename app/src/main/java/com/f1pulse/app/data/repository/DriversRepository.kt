package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.toEntity
import com.f1pulse.app.data.mapper.toTeamInfo
import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.data.remote.openf1.OpenF1Api
import com.f1pulse.app.database.dao.ConstructorDao
import com.f1pulse.app.database.dao.DriverDao
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.entity.DriverEntity
import com.f1pulse.app.database.entity.LastUpdatedEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriversRepository @Inject constructor(
    private val driverDao: DriverDao,
    private val constructorDao: ConstructorDao,
    private val jolpicaApi: JolpicaApi,
    private val openF1Api: OpenF1Api,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val DRIVERS_TTL = 6L * 60 * 60 * 1000 // 6h
    }

    fun observeDrivers(season: Int): Flow<List<DriverEntity>> = driverDao.observeAll(season)

    fun observeDriver(season: Int, driverId: String): Flow<DriverEntity?> =
        driverDao.observeDriver(season, driverId)

    suspend fun getDriver(season: Int, driverId: String): DriverEntity? =
        driverDao.getDriver(season, driverId)

    suspend fun refreshDrivers(season: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "drivers_$season"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val resp = jolpicaApi.getDrivers(season.toString())
            val drivers = resp.mrData.driverTable?.drivers.orEmpty().map { it.toEntity(season) }
            if (drivers.isNotEmpty()) driverDao.upsertAll(drivers)

            // Enrich with OpenF1 team colour + headshot (failure non-fatal).
            try {
                val openDrivers = openF1Api.getDrivers(mapOf("year" to season.toString()))
                val byNumber = openDrivers.map { it.toTeamInfo() }.associateBy { it.driverNumber }
                val enriched = drivers.map { d ->
                    val info = d.permanentNumber?.let { byNumber[it] }
                    if (info != null) d.copy(
                        teamName = info.teamName ?: d.teamName,
                        teamColourHex = info.teamColourHex ?: d.teamColourHex,
                        headshotUrl = info.headshotUrl ?: d.headshotUrl,
                        countryCode = info.countryCode ?: d.countryCode,
                    ) else d
                }
                driverDao.upsertAll(enriched)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // OpenF1 enrichment optional.
            }

            metaDao.upsert(LastUpdatedEntity(key, now, DRIVERS_TTL))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh drivers")
        }
    }
}
