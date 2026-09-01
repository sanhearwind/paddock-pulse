package com.f1pulse.app.core.time

import java.time.Instant

/** Central race-window policy used by app screens, repositories, and widgets. */
object RaceTiming {
    const val ESTIMATED_RACE_DURATION_SECONDS: Long = 3 * 60 * 60

    fun isLive(start: Instant?, now: Instant = Instant.now()): Boolean =
        start != null && !now.isBefore(start) && now.isBefore(start.plusSeconds(ESTIMATED_RACE_DURATION_SECONDS))

    fun isFinished(startMillis: Long, nowMillis: Long): Boolean =
        nowMillis >= startMillis + ESTIMATED_RACE_DURATION_SECONDS * 1_000

    fun isCurrentOrUpcoming(startMillis: Long, nowMillis: Long): Boolean =
        !isFinished(startMillis, nowMillis)
}
