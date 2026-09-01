package com.f1pulse.app.domain.usecase

import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.SessionDetailRepository
import com.f1pulse.app.domain.model.Weather
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveWeatherUseCase @Inject constructor(
    private val sessionDetailRepository: SessionDetailRepository,
) {
    operator fun invoke(sessionKey: Int): Flow<Weather?> =
        sessionDetailRepository.observeWeather(sessionKey).map { it?.toDomain() }
}
