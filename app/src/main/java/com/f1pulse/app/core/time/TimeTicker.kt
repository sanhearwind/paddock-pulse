package com.f1pulse.app.core.time

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** Emits the current epoch time once a minute while the flow is being collected. */
fun minuteTicker(): Flow<Long> = flow {
    while (currentCoroutineContext().isActive) {
        emit(TimeFormatter.now().toEpochMilli())
        delay(60_000L)
    }
}