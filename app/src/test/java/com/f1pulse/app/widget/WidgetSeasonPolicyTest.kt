package com.f1pulse.app.widget

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSeasonPolicyTest {
    @Test
    fun january_usesCurrentYearForScheduleAndPreviousYearForStandings() {
        val now = LocalDate.of(2027, 1, 15)

        assertEquals(2027, WidgetDataLoader.scheduleSeason(now))
        assertEquals(2026, WidgetDataLoader.standingsSeason(now))
    }

    @Test
    fun march_usesCurrentYearForBothDataSets() {
        val now = LocalDate.of(2027, 3, 1)

        assertEquals(2027, WidgetDataLoader.scheduleSeason(now))
        assertEquals(2027, WidgetDataLoader.standingsSeason(now))
    }
}