package com.f1pulse.app.core.time

import com.f1pulse.app.domain.model.RaceSessions
import com.f1pulse.app.domain.model.SessionType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RaceWeekendScheduleTest {
    @Test
    fun regularWeekend_isSortedByActualStartTime() {
        val sessions = RaceSessions(
            fp1 = instant("2026-08-21T12:00:00Z"),
            fp2 = instant("2026-08-22T11:00:00Z"),
            fp3 = instant("2026-08-21T16:00:00Z"),
            qualifying = instant("2026-08-22T15:00:00Z"),
            sprint = null,
            sprintQualifying = null,
            race = instant("2026-08-23T14:00:00Z"),
        )

        assertEquals(
            listOf(SessionType.FP1, SessionType.FP3, SessionType.FP2, SessionType.QUALIFYING, SessionType.RACE),
            sessions.chronologicalSchedule().map { it.type },
        )
    }

    @Test
    fun sprintWeekend_excludesFp2AndFp3AndUsesChronologicalOrder() {
        val sessions = RaceSessions(
            fp1 = instant("2026-08-21T12:00:00Z"),
            fp2 = instant("2026-08-21T16:00:00Z"),
            fp3 = instant("2026-08-22T11:00:00Z"),
            qualifying = instant("2026-08-22T15:00:00Z"),
            sprint = instant("2026-08-22T11:00:00Z"),
            sprintQualifying = instant("2026-08-21T16:00:00Z"),
            race = instant("2026-08-23T14:00:00Z"),
        )

        assertEquals(
            listOf(
                SessionType.FP1,
                SessionType.SPRINT_QUALIFYING,
                SessionType.SPRINT,
                SessionType.QUALIFYING,
                SessionType.RACE,
            ),
            sessions.chronologicalSchedule().map { it.type },
        )
    }

    private fun instant(value: String) = Instant.parse(value)
}
