package com.f1pulse.app.domain.usecase

import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.ResultsRepository
import com.f1pulse.app.domain.model.RaceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * @param sessionType one of "RACE", "SPRINT", "QUALIFYING".
 */
class GetRaceResultsUseCase @Inject constructor(
    private val resultsRepository: ResultsRepository,
) {
    operator fun invoke(season: Int, round: Int, sessionType: String): Flow<List<RaceResult>> =
        resultsRepository.observeResults(season, round, sessionType).map { list ->
            list.map { it.toDomain() }
        }
}
