package com.f1pulse.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveBackButtonTest {
    @Test
    fun `white background uses black icon`() {
        assertEquals(Color.Black, adaptiveBackIconColor(Color.White))
    }

    @Test
    fun `dark background uses white icon`() {
        assertEquals(Color.White, adaptiveBackIconColor(Color(0xFF121212)))
    }
}
