package com.f1pulse.app.ui.racedetail

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentSessionDisclosureTextTest {
    @Test
    fun disclosureOnlyControlsTheCurrentSessionRanking() {
        assertEquals("SHOW RANKING", currentSessionDisclosureText(expanded = false))
        assertEquals("HIDE RANKING", currentSessionDisclosureText(expanded = true))
    }
}
