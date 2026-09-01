package com.f1pulse.app.domain.usecase

import com.f1pulse.app.core.time.minuteTicker
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.CircuitRepository
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.domain.model.Race
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetRaceScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val circuitRepository: CircuitRepository,
) {
    operator fun invoke(season: Int): Flow<List<Race>> {
        return combine(scheduleRepository.observeSchedule(season), minuteTicker()) { list, now ->
            list.map { e ->
                e.toDomain(
                    sessions = emptyList(),
                    circuitMeta = circuitRepository.getCircuitMeta(e.circuitId),
                    now = now,
                )
            }
        }
    }
}
