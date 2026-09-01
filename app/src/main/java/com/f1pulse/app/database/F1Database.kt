package com.f1pulse.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.f1pulse.app.database.dao.ConstructorDao
import com.f1pulse.app.database.dao.DriverDao
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.RaceDao
import com.f1pulse.app.database.dao.ResultDao
import com.f1pulse.app.database.dao.SessionDao
import com.f1pulse.app.database.dao.SessionResultDao
import com.f1pulse.app.database.dao.StandingDao
import com.f1pulse.app.database.dao.WeatherDao
import com.f1pulse.app.database.entity.ConstructorEntity
import com.f1pulse.app.database.entity.ConstructorStandingEntity
import com.f1pulse.app.database.entity.DriverEntity
import com.f1pulse.app.database.entity.DriverStandingEntity
import com.f1pulse.app.database.entity.LastUpdatedEntity
import com.f1pulse.app.database.entity.RaceEntity
import com.f1pulse.app.database.entity.ResultEntity
import com.f1pulse.app.database.entity.SessionEntity
import com.f1pulse.app.database.entity.SessionResultEntity
import com.f1pulse.app.database.entity.WeatherEntity

@Database(
    entities = [
        RaceEntity::class,
        SessionEntity::class,
        DriverEntity::class,
        ConstructorEntity::class,
        DriverStandingEntity::class,
        ConstructorStandingEntity::class,
        ResultEntity::class,
        WeatherEntity::class,
        SessionResultEntity::class,
        LastUpdatedEntity::class,
    ],
    // v2: added `session_result` (OpenF1 per-session classification, incl. practice).
    // Every table here is a re-fetchable cache, so DatabaseModule falls back to a
    // destructive migration rather than carrying migration scripts.
    version = 2,
    exportSchema = true,
)
abstract class F1Database : RoomDatabase() {
    abstract fun raceDao(): RaceDao
    abstract fun sessionDao(): SessionDao
    abstract fun driverDao(): DriverDao
    abstract fun constructorDao(): ConstructorDao
    abstract fun standingDao(): StandingDao
    abstract fun resultDao(): ResultDao
    abstract fun weatherDao(): WeatherDao
    abstract fun sessionResultDao(): SessionResultDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "f1_pulse.db"
    }
}
