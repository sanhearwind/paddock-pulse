package com.f1pulse.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.f1pulse.app.database.entity.LastUpdatedEntity

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LastUpdatedEntity)

    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): LastUpdatedEntity?

    /** Returns 1 if the cached entry is still within its TTL, 0 otherwise. */
    @Query("SELECT EXISTS(SELECT 1 FROM meta WHERE `key` = :key AND lastUpdated + ttlMillis > :now)")
    suspend fun isFresh(key: String, now: Long): Int
}
