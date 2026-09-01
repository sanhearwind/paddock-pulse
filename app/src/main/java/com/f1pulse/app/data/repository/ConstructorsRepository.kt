package com.f1pulse.app.data.repository

import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.core.network.ConnectivityMonitor
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.data.mapper.toEntity
import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.database.dao.ConstructorDao
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.entity.ConstructorEntity
import com.f1pulse.app.database.entity.LastUpdatedEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConstructorsRepository @Inject constructor(
    private val constructorDao: ConstructorDao,
    private val jolpicaApi: JolpicaApi,
    private val metaDao: MetaDao,
    private val connectivityMonitor: ConnectivityMonitor,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    companion object {
        private val CTOR_TTL = 6L * 60 * 60 * 1000 // 6h
    }

    fun observeConstructors(season: Int): Flow<List<ConstructorEntity>> =
        constructorDao.observeAll(season)

    suspend fun refreshConstructors(season: Int, force: Boolean = false): Resource<Unit> = withContext(io) {
        val key = "constructors_$season"
        val now = TimeFormatter.now().toEpochMilli()
        if (!force && metaDao.isFresh(key, now) == 1) return@withContext Resource.Success(Unit)
        if (!connectivityMonitor.isOnline()) return@withContext Resource.Error("Offline")

        try {
            val resp = jolpicaApi.getConstructors(season.toString())
            val list = resp.mrData.constructorTable?.constructors.orEmpty().map { it.toEntity(season) }
            if (list.isNotEmpty()) constructorDao.upsertAll(list)
            metaDao.upsert(LastUpdatedEntity(key, now, CTOR_TTL))
            Resource.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Failed to refresh constructors")
        }
    }
}
