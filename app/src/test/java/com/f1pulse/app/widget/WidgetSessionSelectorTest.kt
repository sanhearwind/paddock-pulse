package com.f1pulse.app.widget

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSessionSelectorTest {
    private val fp1 = WidgetSession("FP1", "FP1", Instant.parse("2026-07-24T11:00:00Z"), 3_600)
    private val qualifying = WidgetSession("QUALI", "Q", Instant.parse("2026-07-25T14:00:00Z"), 5_400)
    private val race = WidgetSession("RACE", "R", Instant.parse("2026-07-26T13:00:00Z"), 10_800)
    private val sessions = listOf(race, fp1, qualifying)

    @Test
    fun beforeWeekend_selectsFirstUpcomingSession() {
        val result = WidgetSessionSelector.select(sessions, Instant.parse("2026-07-24T10:00:00Z"))

        assertNull(result.active)
        assertSame(fp1, result.next)
    }

    @Test
    fun duringQualifying_selectsCurrentSessionAndKeepsRaceAsNext() {
        val now = Instant.parse("2026-07-25T14:30:00Z")
        val result = WidgetSessionSelector.select(sessions, now)

        assertSame(qualifying, result.active)
        assertSame(race, result.next)
        assertTrue(qualifying.isActive(now))
        assertFalse(qualifying.isCompleted(now))
    }

    @Test
    fun afterSessionEnd_movesToFollowingSession() {
        val now = Instant.parse("2026-07-24T12:00:00Z")
        val result = WidgetSessionSelector.select(sessions, now)

        assertNull(result.active)
        assertSame(qualifying, result.next)
        assertTrue(fp1.isCompleted(now))
    }

    @Test
    fun afterRaceEnd_hasNoActiveOrUpcomingSession() {
        val result = WidgetSessionSelector.select(sessions, Instant.parse("2026-07-26T16:00:00Z"))

        assertNull(result.active)
        assertNull(result.next)
        assertEquals(race.endInstant, Instant.parse("2026-07-26T16:00:00Z"))
    }
}