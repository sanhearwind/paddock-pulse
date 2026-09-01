package com.f1pulse.app.database.entity

import androidx.room.Entity

@Entity(tableName = "driver_standing", primaryKeys = ["season", "driverId"])
data class DriverStandingEntity(
    val season: Int,
    val position: Int,
    val points: Double,
    val wins: Int,
    val driverId: String,
    val constructorId: String?,
    val driverName: String,
    val driverCode: String?,
    val constructorName: String?,
    val teamColourHex: String?,
    val lastUpdated: Long,
)

@Entity(tableName = "constructor_standing", primaryKeys = ["season", "constructorId"])
data class ConstructorStandingEntity(
    val season: Int,
    val position: Int,
    val points: Double,
    val wins: Int,
    val constructorId: String,
    val constructorName: String,
    val teamColourHex: String?,
    val lastUpdated: Long,
)
