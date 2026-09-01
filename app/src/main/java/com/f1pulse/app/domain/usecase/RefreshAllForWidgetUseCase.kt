package com.f1pulse.app.domain.usecase

import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.core.time.SessionSelector
import com.f1pulse.app.core.time.SessionWindow
import com.f1pulse.app.data.mapper.toDomain
import com.f1pulse.app.data.repository.ScheduleRepository
import com.f1pulse.app.data.repository.SessionDetailRepository
import com.f1pulse.app.data.repository.StandingsRepository
import kotlinx.coroutines.CancellationException
import java.time.Instant
import javax.inject.Inject

/**
 * Used by [com.f1pulse.app.widget.worker.WidgetRefreshWorker] to pull all widget-relevant
 * data (schedule + standings) in one shot. Either source is required by at least one
 * widget, so any source failure is surfaced and retried.
 *
 * The most recently finished session's classification is also refreshed, for the
 * last-session widget. That one is best-effort: it depends on OpenF1 having published the
 * session, which the free tier only does ~30 minutes after it ends, so a miss is expected
 * and must not mark the whole refresh as failed.
 */
class RefreshAllForWidgetUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val standingsRepository: StandingsRepository,
    private val sessionDetailRepository: SessionDetailRepository,
) {
    suspend operator fun invoke(scheduleSeason: Int, standingsSeason: Int = scheduleSeason): Resource<Unit> {
        val schedule = scheduleRepository.refreshSchedule(scheduleSeason, force = true)
        val standings = standingsRepository.refreshStandings(standingsSeason, force = true)

        try {
            refreshLastSessionResult(scheduleSeason)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Best-effort enrichment; never blocks the widget refresh.
        }

        return when {
            schedule is Resource.Error -> schedule
            standings is Resource.Error -> standings
            else -> Resource.Success(Unit)
        }
    }

    private suspend fun refreshLastSessionResult(season: Int) {
        val sessions = scheduleRepository.getSessionsForSeason(season)
            .map { it.toDomain() }
            .filter { it.sessionType != null }
        if (sessions.isEmpty()) return

        val lastFinished = SessionSelector.select(sessions, Instant.now()) { detail ->
            SessionWindow(
                start = detail.dateStart,
                end = detail.dateEnd ?: detail.dateStart.plusSeconds(
                    SessionSelector.estimatedDurationSeconds(detail.sessionType),
                ),
            )
        }.lastFinished ?: return

        sessionDetailRepository.refreshSessionResult(lastFinished.sessionKey)
    }
}
