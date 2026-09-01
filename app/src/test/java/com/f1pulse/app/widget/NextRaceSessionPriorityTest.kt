package com.f1pulse.app.widget

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NextRaceSessionPriorityTest {
    private val fp1 = WidgetSession("FP1", "FP1", Instant.parse("2026-07-24T11:00:00Z"), 3_600)
    private val fp2 = WidgetSession("FP2", "FP2", Instant.parse("2026-07-24T15:00:00Z"), 3_600)
    private val fp3 = WidgetSession("FP3", "FP3", Instant.parse("2026-07-25T10:00:00Z"), 3_600)
    private val qualifying = WidgetSession("QUALI", "Q", Instant.parse("2026-07-25T14:00:00Z"), 5_400)
    private val race = WidgetSession("RACE", "R", Instant.parse("2026-07-26T13:00:00Z"), 10_800)
    private val weekend = listOf(fp1, fp2, fp3, qualifying, race)

    @Test
    fun twoRows_keepQualifyingAndRaceInChronologicalOrder() {
        assertEquals(listOf(qualifying, race), weekend.prioritizeSessions(maxSessions = 2))
    }

    @Test
    fun extraHeightAddsPracticeBeforeQualifyingAndRace() {
        assertEquals(listOf(fp3, qualifying, race), weekend.prioritizeSessions(maxSessions = 3))
        assertEquals(weekend, weekend.prioritizeSessions(maxSessions = 5))
    }
}