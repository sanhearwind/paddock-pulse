package com.f1pulse.app.domain.usecase

import com.f1pulse.app.core.time.SessionSelector
import com.f1pulse.app.core.time.SessionWindow
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.data.repository.SessionDetailRepository
import com.f1pulse.app.domain.model.SessionDetail
import com.f1pulse.app.domain.model.SessionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * A single session of a race weekend together with its classification.
 *
 * [results] stays empty until OpenF1 publishes the session — on the free tier that is roughly
 * 30 minutes after the session ends, since the window from 30 minutes before the start until
 * 30 minutes after the end is paid-only live data.
 */
data class SessionWithResults(
    val detail: SessionDetail,
    val results: List<SessionResult>,
) {
    val window: SessionWindow
        get() = SessionWindow(
            start = detail.dateStart,
            end = detail.dateEnd
                ?: detail.dateStart.plusSeconds(
                    SessionSelector.estimatedDurationSeconds(detail.sessionType),
                ),
        )
}

/**
 * Every classified session of a round, in chronological order, each with whatever results
 * have been published. Feeds both the "current session" card and the per-session result tabs,
 * so the two can never disagree.
 */
class GetRoundSessionResultsUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val sessionDetailRepository: SessionDetailRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(season: Int, round: Int): Flow<List<SessionWithResults>> =
        scheduleRepository.observeSessions(season, round)
            .map { entities ->
                entities.map { it.toDomain() }
                    // Unclassifiable sessions are pre-season testing; they belong to no round.
                    .filter { it.sessionType != null }
                    .sortedBy { it.dateStart }
            }
            .flatMapLatest { sessions ->
                if (sessions.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        sessions.map { detail ->
                            sessionDetailRepository.observeSessionResult(detail.sessionKey)
                                .map { rows -> SessionWithResults(detail, rows.map { it.toDomain() }) }
                        },
                    ) { it.toList() }
                }
            }
}
