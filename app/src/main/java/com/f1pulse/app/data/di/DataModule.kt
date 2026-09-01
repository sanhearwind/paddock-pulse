package com.f1pulse.app.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * All repositories ([com.f1pulse.app.data.repository.ScheduleRepository] et al.) and
 * [com.f1pulse.app.data.prefs.SettingsDataStore] self-provide via `@Inject constructor`,
 * so this module needs no `@Provides` entries. It exists to document the data-layer DI
 * wiring and keep the package structure aligned with the architecture.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule
