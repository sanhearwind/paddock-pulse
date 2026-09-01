package com.f1pulse.app.database.entity

import androidx.room.Entity

@Entity(tableName = "driver", primaryKeys = ["season", "driverId"])
data class DriverEntity(
    val season: Int,
    val driverId: String,
    val permanentNumber: Int?,
    val code: String?,
    val givenName: String,
    val familyName: String,
    val fullName: String,
    val nationality: String,
    val dateOfBirth: String?,
    val teamName: String?,
    val teamColourHex: String?,
    val headshotUrl: String?,
    val countryCode: String?,
)

@Entity(tableName = "constructor", primaryKeys = ["season", "constructorId"])
data class ConstructorEntity(
    val season: Int,
    val constructorId: String,
    val name: String,
    val nationality: String,
    val colourHex: String?,
)
