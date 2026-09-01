package com.f1pulse.app.core.time

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Drives the race-detail "current session" card: a session stays selected while it runs, and
 * once it ends it — and its results — stay on screen until the next session starts.
 */
class SessionSelectorTest {

    private data class Fake(val name: String, val start: String, val durationSeconds: Long)

    private val fp1 = Fake("FP1", "2026-07-24T11:00:00Z", 3_600)
    private val fp2 = Fake("FP2", "2026-07-24T15:00:00Z", 3_600)
    private val qualifying = Fake("Q", "2026-07-25T14:00:00Z", 5_400)
    private val race = Fake("R", "2026-07-26T13:00:00Z", 10_800)

    // Deliberately out of order: selection must not depend on input ordering.
    private val sessions = listOf(race, fp2, qualifying, fp1)

    private fun window(f: Fake) = SessionWindow(
        start = Instant.parse(f.start),
        end = Instant.parse(f.start).plusSeconds(f.durationSeconds),
    )

    private fun currentAt(iso: String) =
        SessionSelector.current(sessions, Instant.parse(iso), ::window)

    @Test
    fun beforeTheWeekendTheFirstSessionIsUpcoming() {
        val current = currentAt("2026-07-24T09:00:00Z")!!
        assertSame(fp1, current.session)
        assertEquals(SessionPhase.UPCOMING, current.phase)
    }

    @Test
    fun aRunningSessionIsSelected() {
        val current = currentAt("2026-07-24T11:30:00Z")!!
        assertSame(fp1, current.session)
        assertEquals(SessionPhase.IN_PROGRESS, current.phase)
    }

    @Test
    fun afterASessionEndsItStaysSelectedUntilTheNextOneStarts() {
        // FP1 ended at 12:00, FP2 does not start until 15:00 — the gap belongs to FP1.
        val justAfter = currentAt("2026-07-24T12:01:00Z")!!
        assertSame(fp1, justAfter.session)
        assertEquals(SessionPhase.FINISHED, justAfter.phase)

        val muchLater = currentAt("2026-07-24T14:59:00Z")!!
        assertSame(fp1, muchLater.session)
        assertEquals(SessionPhase.FINISHED, muchLater.phase)
    }

    @Test
    fun theNextSessionTakesOverTheMomentItStarts() {
        val current = currentAt("2026-07-24T15:00:00Z")!!
        assertSame(fp2, current.session)
        assertEquals(SessionPhase.IN_PROGRESS, current.phase)
    }

    @Test
    fun overnightGapsKeepTheLastFinishedSession() {
        // Between Friday's FP2 and Saturday's qualifying.
        val current = currentAt("2026-07-25T06:00:00Z")!!
        assertSame(fp2, current.session)
        assertEquals(SessionPhase.FINISHED, current.phase)
    }

    @Test
    fun afterTheRaceTheRaceRemainsSelected() {
        val current = currentAt("2026-07-27T09:00:00Z")!!
        assertSame(race, current.session)
        assertEquals(SessionPhase.FINISHED, current.phase)
    }

    @Test
    fun noSessionsYieldsNothing() {
        assertNull(SessionSelector.current(emptyList<Fake>(), Instant.parse("2026-07-26T13:00:00Z"), ::window))
    }

    @Test
    fun selectExposesActiveNextAndLastFinishedTogether() {
        val selection = SessionSelector.select(sessions, Instant.parse("2026-07-25T14:30:00Z"), ::window)
        assertSame(qualifying, selection.active)
        assertSame(race, selection.next)
        // Qualifying is running, so the last *finished* session is still FP2.
        assertSame(fp2, selection.lastFinished)
    }
}
