package com.f1pulse.app.core.time

import com.f1pulse.app.domain.model.SessionType
import java.time.Instant

/** Where a session sits relative to now. */
enum class SessionPhase { UPCOMING, IN_PROGRESS, FINISHED }

/** A session reduced to the only thing selection cares about: when it runs. */
data class SessionWindow(val start: Instant, val end: Instant)

data class SessionSelection<T>(
    /** The session currently running, if any. */
    val active: T?,
    /** The next session that has not started. */
    val next: T?,
    /** The most recently finished session. */
    val lastFinished: T?,
)

/** A session paired with the phase it is in — what the UI should be showing right now. */
data class CurrentSession<T>(val session: T, val phase: SessionPhase)

/**
 * Shared session-window policy for app screens and widgets.
 *
 * OpenF1 supplies `date_end` for most sessions, but not always, so callers that only know a
 * start time can fall back to [estimatedDurationSeconds].
 */
object SessionSelector {

    /** Typical session lengths, used when the feed gives us no explicit end time. */
    fun estimatedDurationSeconds(type: SessionType?): Long = when (type) {
        SessionType.RACE -> RaceTiming.ESTIMATED_RACE_DURATION_SECONDS
        SessionType.QUALIFYING -> 90 * 60
        SessionType.SPRINT -> 60 * 60
        SessionType.SPRINT_QUALIFYING -> 60 * 60
        SessionType.FP1, SessionType.FP2, SessionType.FP3 -> 60 * 60
        null -> 60 * 60
    }

    fun <T> select(
        items: List<T>,
        now: Instant,
        window: (T) -> SessionWindow,
    ): SessionSelection<T> {
        val ordered = items.sortedBy { window(it).start }
        return SessionSelection(
            active = ordered.firstOrNull {
                val w = window(it)
                !now.isBefore(w.start) && now.isBefore(w.end)
            },
            next = ordered.firstOrNull { now.isBefore(window(it).start) },
            lastFinished = ordered.lastOrNull { !now.isBefore(window(it).end) },
        )
    }

    /**
     * The session the UI should surface right now:
     *
     *  1. a session in progress, if there is one;
     *  2. otherwise the most recently finished session — which stays on screen until the
     *     next session begins, so results remain visible for the rest of the weekend;
     *  3. otherwise the next upcoming session (nothing has run yet).
     *
     * Returns null only when there are no sessions at all.
     */
    fun <T> current(
        items: List<T>,
        now: Instant,
        window: (T) -> SessionWindow,
    ): CurrentSession<T>? {
        val selection = select(items, now, window)
        selection.active?.let { return CurrentSession(it, SessionPhase.IN_PROGRESS) }
        selection.lastFinished?.let { return CurrentSession(it, SessionPhase.FINISHED) }
        selection.next?.let { return CurrentSession(it, SessionPhase.UPCOMING) }
        return null
    }
}
