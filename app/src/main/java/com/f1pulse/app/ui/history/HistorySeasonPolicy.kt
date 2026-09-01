package com.f1pulse.app.ui.history

import java.time.Year

/** Bounds and defaults for the historical championship archive. */
object HistorySeasonPolicy {
    const val FIRST_CHAMPIONSHIP_YEAR = 1950

    fun currentYear(): Int = Year.now().value

    fun defaultSeason(currentYear: Int = currentYear()): Int =
        (currentYear - 1).coerceAtLeast(FIRST_CHAMPIONSHIP_YEAR)

    fun availableSeasons(currentYear: Int = currentYear()): List<Int> =
        (currentYear downTo FIRST_CHAMPIONSHIP_YEAR).toList()

    fun clamp(season: Int, currentYear: Int = currentYear()): Int =
        season.coerceIn(FIRST_CHAMPIONSHIP_YEAR, currentYear)
}
