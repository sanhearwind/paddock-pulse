package com.f1pulse.app.widget

import kotlin.math.floor
import kotlin.math.min

internal object StandingsWidgetLayout {
    private const val FIXED_VERTICAL_SPACE_DP = 52f
    private const val MIN_ROW_SLOT_DP = 38f

    fun visibleRowCount(heightDp: Float, dataCount: Int): Int {
        if (dataCount <= 0) return 0
        val listBudget = (heightDp - FIXED_VERTICAL_SPACE_DP).coerceAtLeast(MIN_ROW_SLOT_DP)
        val capacity = floor(listBudget / MIN_ROW_SLOT_DP).toInt().coerceAtLeast(1)
        return min(capacity, dataCount)
    }
}