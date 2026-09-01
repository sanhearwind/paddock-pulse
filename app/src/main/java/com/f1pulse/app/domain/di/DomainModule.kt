package com.f1pulse.app.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * All use cases ([com.f1pulse.app.domain.usecase.*]) self-provide via `@Inject constructor`,
 * so this module needs no `@Provides` entries. It documents the domain-layer DI wiring.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule
