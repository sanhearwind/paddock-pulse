package com.f1pulse.app.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySeasonPolicyTest {
    @Test
    fun defaultsToLatestCompletedSeason() {
        assertEquals(2025, HistorySeasonPolicy.defaultSeason(currentYear = 2026))
    }

    @Test
    fun exposesChampionshipYearsNewestFirst() {
        val seasons = HistorySeasonPolicy.availableSeasons(currentYear = 1953)

        assertEquals(listOf(1953, 1952, 1951, 1950), seasons)
    }

    @Test
    fun clampsRequestsToKnownChampionshipRange() {
        assertEquals(1950, HistorySeasonPolicy.clamp(1949, currentYear = 2026))
        assertEquals(2026, HistorySeasonPolicy.clamp(2030, currentYear = 2026))
    }
}
