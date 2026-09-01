package com.f1pulse.app.widget

import android.content.Context
import com.f1pulse.app.core.time.TimeZoneResolver
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.data.repository.SessionDetailRepository
import com.f1pulse.app.domain.usecase.GetConstructorStandingsUseCase
import com.f1pulse.app.domain.usecase.GetDriverStandingsUseCase
import com.f1pulse.app.domain.usecase.GetNextRaceUseCase
import com.f1pulse.app.domain.usecase.RefreshAllForWidgetUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Entry point exposing app-singleton dependencies to Glance widgets and
 * [androidx.work.Worker]s, which cannot participate in Hilt member injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getNextRaceUseCase(): GetNextRaceUseCase
    fun getDriverStandingsUseCase(): GetDriverStandingsUseCase
    fun getConstructorStandingsUseCase(): GetConstructorStandingsUseCase
    fun refreshAllForWidgetUseCase(): RefreshAllForWidgetUseCase
    fun scheduleRepository(): ScheduleRepository
    fun sessionDetailRepository(): SessionDetailRepository
    fun settingsDataStore(): SettingsDataStore
    fun timeZoneResolver(): TimeZoneResolver
}

object WidgetEntryPointAccess {
    fun from(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
}
