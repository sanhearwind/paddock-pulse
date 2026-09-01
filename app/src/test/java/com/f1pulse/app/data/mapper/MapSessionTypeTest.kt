package com.f1pulse.app.data.mapper

import com.f1pulse.app.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * OpenF1's `session_type` only distinguishes Practice / Qualifying / Race, so the specific
 * session has to come from `session_name`. Getting this wrong is not cosmetic: an earlier
 * `else -> RACE` fallback classified every practice session as the race, so the weather
 * lookup read a different session key than the one it wrote and never found a row.
 */
class MapSessionTypeTest {

    @Test
    fun practiceSessionsResolveByName() {
        assertEquals(SessionType.FP1, mapSessionType("Practice", "Practice 1"))
        assertEquals(SessionType.FP2, mapSessionType("Practice", "Practice 2"))
        assertEquals(SessionType.FP3, mapSessionType("Practice", "Practice 3"))
    }

    @Test
    fun qualifyingAndRaceResolve() {
        assertEquals(SessionType.QUALIFYING, mapSessionType("Qualifying", "Qualifying"))
        assertEquals(SessionType.RACE, mapSessionType("Race", "Race"))
    }

    @Test
    fun sprintFormatsAreNotConfusedWithTheirBaseType() {
        // OpenF1 files the sprint under session_type "Race" and sprint quali under "Qualifying".
        assertEquals(SessionType.SPRINT, mapSessionType("Race", "Sprint"))
        assertEquals(SessionType.SPRINT_QUALIFYING, mapSessionType("Qualifying", "Sprint Qualifying"))
    }

    @Test
    fun preSeasonTestingIsUnclassified() {
        // Testing days are session_type "Practice" named "Day 1".."Day 3" and belong to no round.
        assertNull(mapSessionType("Practice", "Day 1"))
        assertNull(mapSessionType("Practice", "Day 2"))
        assertNull(mapSessionType("Practice", "Day 3"))
    }

    @Test
    fun unknownPracticeIsNullRatherThanFallingBackToRace() {
        assertNull(mapSessionType("Practice", ""))
        assertNull(mapSessionType("", ""))
        assertNull(mapSessionType("Something New", "Whatever"))
    }

    @Test
    fun namedSessionsAreCaseAndSpacingInsensitive() {
        assertEquals(SessionType.FP1, mapSessionType("practice", "practice 1"))
        assertEquals(SessionType.SPRINT_QUALIFYING, mapSessionType("QUALIFYING", "SPRINTQUALIFYING"))
    }

    @Test
    fun unnamedQualifyingOrRaceStillResolvesFromType() {
        assertEquals(SessionType.QUALIFYING, mapSessionType("Qualifying", ""))
        assertEquals(SessionType.RACE, mapSessionType("Race", ""))
    }
}
