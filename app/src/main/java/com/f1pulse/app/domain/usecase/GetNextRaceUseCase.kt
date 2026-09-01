package com.f1pulse.app.domain.usecase

import com.f1pulse.app.core.time.RaceTiming
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.minuteTicker
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.CircuitRepository
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.domain.model.Race
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNextRaceUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val circuitRepository: CircuitRepository,
) {
    operator fun invoke(season: Int): Flow<Race?> {
        return combine(scheduleRepository.observeSchedule(season), minuteTicker()) { races, now ->
            races.filter { RaceTiming.isCurrentOrUpcoming(it.raceDateUtc, now) }.minByOrNull { it.raceDateUtc }
        }.map { e ->
            e?.let {
                val meta = circuitRepository.getCircuitMeta(it.circuitId)
                it.toDomain(sessions = emptyList(), circuitMeta = meta, now = TimeFormatter.now().toEpochMilli())
            }
        }
    }
}
