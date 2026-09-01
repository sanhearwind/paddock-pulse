package com.f1pulse.app.domain.usecase

import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.StandingsRepository
import com.f1pulse.app.domain.model.ConstructorStanding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetConstructorStandingsUseCase @Inject constructor(
    private val standingsRepository: StandingsRepository,
) {
    operator fun invoke(season: Int): Flow<List<ConstructorStanding>> =
        standingsRepository.observeConstructorStandings(season).map { list ->
            list.map { it.toDomain() }
        }
}
