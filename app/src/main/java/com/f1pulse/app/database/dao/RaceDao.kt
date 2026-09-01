package com.f1pulse.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.f1pulse.app.database.entity.RaceEntity
import com.f1pulse.app.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(races: List<RaceEntity>)

    @Query("SELECT * FROM race WHERE season = :season ORDER BY round ASC")
    fun observeAll(season: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM race WHERE season = :season AND raceDateUtc > :now ORDER BY raceDateUtc ASC LIMIT 1")
    fun observeNextRace(season: Int, now: Long): Flow<RaceEntity?>

    @Query("SELECT * FROM race WHERE season = :season AND raceDateUtc <= :now ORDER BY raceDateUtc DESC LIMIT 1")
    fun observeLastRace(season: Int, now: Long): Flow<RaceEntity?>

    @Query("SELECT * FROM race WHERE season = :season AND round = :round LIMIT 1")
    fun observeRace(season: Int, round: Int): Flow<RaceEntity?>

    @Query("SELECT * FROM race WHERE season = :season AND round = :round LIMIT 1")
    suspend fun getRace(season: Int, round: Int): RaceEntity?

    @Query("SELECT * FROM race WHERE season = :season AND openF1MeetingKey = :meetingKey LIMIT 1")
    suspend fun getRaceByMeetingKey(season: Int, meetingKey: Int): RaceEntity?
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Query("SELECT * FROM session WHERE season = :season ORDER BY round ASC, dateStartUtc ASC")
    fun observeForSeason(season: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE season = :season ORDER BY dateStartUtc ASC")
    suspend fun getForSeason(season: Int): List<SessionEntity>

    @Query("SELECT * FROM session WHERE season = :season AND round = :round ORDER BY dateStartUtc ASC")
    fun observeForRound(season: Int, round: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE meetingKey = :meetingKey ORDER BY dateStartUtc ASC")
    suspend fun getForMeeting(meetingKey: Int): List<SessionEntity>

    @Query("SELECT * FROM session WHERE sessionKey = :key LIMIT 1")
    suspend fun getByKey(key: Int): SessionEntity?
}
