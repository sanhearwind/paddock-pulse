package com.f1pulse.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.f1pulse.app.database.entity.ConstructorEntity
import com.f1pulse.app.database.entity.DriverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(drivers: List<DriverEntity>)

    @Query("SELECT * FROM driver WHERE season = :season ORDER BY familyName ASC")
    fun observeAll(season: Int): Flow<List<DriverEntity>>

    @Query("SELECT * FROM driver WHERE season = :season AND driverId = :driverId LIMIT 1")
    fun observeDriver(season: Int, driverId: String): Flow<DriverEntity?>

    @Query("SELECT * FROM driver WHERE season = :season AND driverId = :driverId LIMIT 1")
    suspend fun getDriver(season: Int, driverId: String): DriverEntity?
}

@Dao
interface ConstructorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(constructors: List<ConstructorEntity>)

    @Query("SELECT * FROM constructor WHERE season = :season ORDER BY name ASC")
    fun observeAll(season: Int): Flow<List<ConstructorEntity>>
}
