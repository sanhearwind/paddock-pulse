package com.f1pulse.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownWidgetLayoutPolicyTest {

    @Test
    fun `track layout starts at the wide breakpoint`() {
        assertFalse(CountdownWidgetLayoutPolicy.useTrackLayout(299.9f, thumbnailAvailable = true))
        assertTrue(CountdownWidgetLayoutPolicy.useTrackLayout(300f, thumbnailAvailable = true))
    }

    @Test
    fun `missing thumbnail keeps the legacy layout`() {
        assertFalse(CountdownWidgetLayoutPolicy.useTrackLayout(500f, thumbnailAvailable = false))
    }

    @Test
    fun `thumbnail path follows the upstream circuit id`() {
        assertEquals(
            "track_thumbnails/sepang.svg",
            CountdownWidgetLayoutPolicy.thumbnailAsset("sepang"),
        )
        assertNull(CountdownWidgetLayoutPolicy.thumbnailAsset("  "))
        assertNull(CountdownWidgetLayoutPolicy.thumbnailAsset(null))
    }
}
