package com.f1pulse.app.data.mapper

import com.f1pulse.app.data.remote.openf1.dto.OpenF1DriverDto
import com.f1pulse.app.data.remote.openf1.dto.OpenF1SessionResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * OpenF1's `session_result` sends `duration` and `gap_to_leader` as a bare number for practice
 * and race sessions, but as a three-element `[Q1, Q2, Q3]` array for qualifying. Both shapes
 * arrive through the same field, so the mapper has to collapse them.
 */
class SessionResultMapperTest {

    private fun dto(
        duration: Any? = null,
        gap: Any? = null,
        position: Int? = 1,
    ) = OpenF1SessionResultDto(
        sessionKey = 11337,
        driverNumber = 1,
        position = position,
        numberOfLaps = 20,
        duration = duration,
        gapToLeader = gap,
    )

    @Test
    fun practiceAndRaceSendABareNumber() {
        val entity = dto(duration = 77.939, gap = 0.117).toEntity(null)!!
        assertEquals(77.939, entity.durationSeconds!!, 1e-9)
        assertEquals(0.117, entity.gapSeconds!!, 1e-9)
    }

    @Test
    fun qualifyingSendsAnArrayAndTheLastSegmentIsTheBest() {
        // [Q1, Q2, Q3] — Q3 is the driver's best effort.
        val entity = dto(duration = listOf(78.277, 77.456, 77.207)).toEntity(null)!!
        assertEquals(77.207, entity.durationSeconds!!, 1e-9)
    }

    @Test
    fun qualifyingArrayWithNullsFallsBackToTheLastSetTime() {
        // Eliminated in Q1: only the first segment has a time.
        val entity = dto(duration = listOf(80.120, null, null)).toEntity(null)!!
        assertEquals(80.120, entity.durationSeconds!!, 1e-9)
    }

    @Test
    fun missingOrUnusableValuesBecomeNull() {
        val entity = dto(duration = null, gap = emptyList<Double>()).toEntity(null)!!
        assertNull(entity.durationSeconds)
        assertNull(entity.gapSeconds)
    }

    @Test
    fun rowsWithoutAPositionAreDropped() {
        assertNull(dto(position = null).toEntity(null))
    }

    @Test
    fun driverDetailsComeFromTheSessionDriverList() {
        val driver = OpenF1DriverDto(
            driverNumber = 1,
            fullName = "Lando NORRIS",
            nameAcronym = "NOR",
            teamName = "McLaren",
            teamColour = "F47600",
        )
        val entity = dto(duration = 77.939).toEntity(driver)!!
        assertEquals("Lando NORRIS", entity.driverName)
        assertEquals("NOR", entity.driverCode)
        assertEquals("McLaren", entity.teamName)
        assertEquals("F47600", entity.teamColourHex)
    }

    @Test
    fun unknownDriverFallsBackToTheCarNumber() {
        // Practice sessions field reserve drivers who may not appear in any driver list.
        val entity = dto(duration = 77.939).toEntity(null)!!
        assertEquals("#1", entity.driverName)
        assertEquals("1", entity.driverCode)
    }
}
