package com.f1pulse.app.domain.usecase

import com.f1pulse.app.core.time.minuteTicker
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.CircuitRepository
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.domain.model.Race
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetRaceDetailUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val circuitRepository: CircuitRepository,
) {
    operator fun invoke(season: Int, round: Int): Flow<Race?> {
        return combine(
            scheduleRepository.observeRace(season, round),
            scheduleRepository.observeSessions(season, round),
            minuteTicker(),
        ) { race, sessions, now ->
            race?.let {
                val meta = circuitRepository.getCircuitMeta(it.circuitId)
                it.toDomain(sessions = sessions, circuitMeta = meta, now = now)
            }
        }
    }
}
