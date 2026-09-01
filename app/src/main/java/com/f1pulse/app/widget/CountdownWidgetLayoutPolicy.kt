package com.f1pulse.app.widget

internal object CountdownWidgetLayoutPolicy {
    const val TRACK_LAYOUT_MIN_WIDTH_DP = 300f

    fun useTrackLayout(widthDp: Float, thumbnailAvailable: Boolean): Boolean =
        thumbnailAvailable && widthDp >= TRACK_LAYOUT_MIN_WIDTH_DP

    fun thumbnailAsset(circuitId: String?): String? =
        circuitId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "track_thumbnails/$it.svg" }
}