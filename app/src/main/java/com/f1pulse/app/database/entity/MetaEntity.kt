package com.f1pulse.app.database.entity

import androidx.room.Entity

/** TTL metadata for cache freshness checks. */
@Entity(tableName = "meta", primaryKeys = ["key"])
data class LastUpdatedEntity(
    val key: String,
    val lastUpdated: Long,
    val ttlMillis: Long,
)
