package com.f1pulse.app.core.time

import com.f1pulse.app.domain.model.RaceSessions
import com.f1pulse.app.domain.model.SessionType
import java.time.Instant

data class ScheduledRaceSession(
    val type: SessionType,
    val instant: Instant,
)

/**
 * Returns only sessions that belong to the weekend format, ordered by start time.
 * A sprint or sprint-qualifying timestamp identifies a sprint weekend; FP2/FP3
 * values are ignored in that format even if an upstream payload leaves them set.
 */
fun RaceSessions.chronologicalSchedule(): List<ScheduledRaceSession> {
    val isSprintWeekend = sprint != null || sprintQualifying != null
    return buildList {
        fp1?.let { add(ScheduledRaceSession(SessionType.FP1, it)) }
        if (!isSprintWeekend) {
            fp2?.let { add(ScheduledRaceSession(SessionType.FP2, it)) }
            fp3?.let { add(ScheduledRaceSession(SessionType.FP3, it)) }
        }
        sprintQualifying?.let { add(ScheduledRaceSession(SessionType.SPRINT_QUALIFYING, it)) }
        sprint?.let { add(ScheduledRaceSession(SessionType.SPRINT, it)) }
        qualifying?.let { add(ScheduledRaceSession(SessionType.QUALIFYING, it)) }
        race?.let { add(ScheduledRaceSession(SessionType.RACE, it)) }
    }.sortedBy(ScheduledRaceSession::instant)
}
