package com.f1pulse.app.database.di

import android.content.Context
import androidx.room.Room
import com.f1pulse.app.database.F1Database
import com.f1pulse.app.database.dao.ConstructorDao
import com.f1pulse.app.database.dao.DriverDao
import com.f1pulse.app.database.dao.MetaDao
import com.f1pulse.app.database.dao.RaceDao
import com.f1pulse.app.database.dao.ResultDao
import com.f1pulse.app.database.dao.SessionDao
import com.f1pulse.app.database.dao.SessionResultDao
import com.f1pulse.app.database.dao.StandingDao
import com.f1pulse.app.database.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): F1Database =
        Room.databaseBuilder(context, F1Database::class.java, F1Database.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun raceDao(db: F1Database): RaceDao = db.raceDao()
    @Provides fun sessionDao(db: F1Database): SessionDao = db.sessionDao()
    @Provides fun driverDao(db: F1Database): DriverDao = db.driverDao()
    @Provides fun constructorDao(db: F1Database): ConstructorDao = db.constructorDao()
    @Provides fun standingDao(db: F1Database): StandingDao = db.standingDao()
    @Provides fun resultDao(db: F1Database): ResultDao = db.resultDao()
    @Provides fun weatherDao(db: F1Database): WeatherDao = db.weatherDao()
    @Provides fun sessionResultDao(db: F1Database): SessionResultDao = db.sessionResultDao()
    @Provides fun metaDao(db: F1Database): MetaDao = db.metaDao()
}
