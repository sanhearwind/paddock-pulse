package com.f1pulse.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class StandingsWidgetLayoutTest {
    @Test
    fun `height growth increases visible standings instead of font size`() {
        assertEquals(4, StandingsWidgetLayout.visibleRowCount(heightDp = 220f, dataCount = 20))
        assertEquals(9, StandingsWidgetLayout.visibleRowCount(heightDp = 400f, dataCount = 20))
    }

    @Test
    fun `visible rows never exceed available standings`() {
        assertEquals(4, StandingsWidgetLayout.visibleRowCount(heightDp = 600f, dataCount = 4))
        assertEquals(0, StandingsWidgetLayout.visibleRowCount(heightDp = 600f, dataCount = 0))
    }
}