package com.f1pulse.app.domain.usecase

import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.DriversRepository
import com.f1pulse.app.domain.model.Driver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDriverDetailUseCase @Inject constructor(
    private val driversRepository: DriversRepository,
) {
    operator fun invoke(season: Int, driverId: String): Flow<Driver?> =
        driversRepository.observeDriver(season, driverId).map { it?.toDomain() }
}
