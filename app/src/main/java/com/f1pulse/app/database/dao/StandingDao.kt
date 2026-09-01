package com.f1pulse.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.f1pulse.app.database.entity.ConstructorStandingEntity
import com.f1pulse.app.database.entity.DriverStandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDriverStandings(rows: List<DriverStandingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConstructorStandings(rows: List<ConstructorStandingEntity>)

    @Query("SELECT * FROM driver_standing WHERE season = :season ORDER BY position ASC")
    fun observeDriverStandings(season: Int): Flow<List<DriverStandingEntity>>

    @Query("SELECT * FROM constructor_standing WHERE season = :season ORDER BY position ASC")
    fun observeConstructorStandings(season: Int): Flow<List<ConstructorStandingEntity>>

    @Query("SELECT * FROM driver_standing WHERE season = :season ORDER BY position ASC LIMIT :limit")
    suspend fun getTopDrivers(season: Int, limit: Int): List<DriverStandingEntity>

    @Query("SELECT * FROM constructor_standing WHERE season = :season ORDER BY position ASC LIMIT :limit")
    suspend fun getTopConstructors(season: Int, limit: Int): List<ConstructorStandingEntity>
}
