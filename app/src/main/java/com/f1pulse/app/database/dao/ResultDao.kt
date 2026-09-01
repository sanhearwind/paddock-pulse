package com.f1pulse.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.f1pulse.app.database.entity.ResultEntity
import com.f1pulse.app.database.entity.SessionResultEntity
import com.f1pulse.app.database.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(results: List<ResultEntity>)

    @Query("SELECT * FROM result WHERE season = :season AND round = :round AND sessionType = :sessionType ORDER BY position ASC")
    fun observeResults(season: Int, round: Int, sessionType: String): Flow<List<ResultEntity>>

    @Query("SELECT * FROM result WHERE season = :season AND round = :round AND sessionType = :sessionType ORDER BY position ASC")
    suspend fun getResults(season: Int, round: Int, sessionType: String): List<ResultEntity>
}

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<WeatherEntity>)

    @Query("SELECT * FROM weather WHERE sessionKey = :key ORDER BY dateUtc DESC LIMIT 1")
    fun observeLatest(key: Int): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather WHERE sessionKey = :key ORDER BY dateUtc DESC LIMIT 1")
    suspend fun getLatest(key: Int): WeatherEntity?
}

@Dao
interface SessionResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<SessionResultEntity>)

    @Query("SELECT * FROM session_result WHERE sessionKey = :key ORDER BY position ASC")
    fun observeForSession(key: Int): Flow<List<SessionResultEntity>>

    @Query("SELECT * FROM session_result WHERE sessionKey = :key ORDER BY position ASC")
    suspend fun getForSession(key: Int): List<SessionResultEntity>
}
