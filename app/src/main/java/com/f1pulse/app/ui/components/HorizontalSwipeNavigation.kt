package com.f1pulse.app.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Detects an intentional horizontal page gesture and delegates the visual
 * transition to the destination container. Keeping gesture recognition and
 * route animation separate prevents a release-time jump or double animation.
 */
fun Modifier.horizontalSwipeNavigation(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
): Modifier = composed {
    val currentLeft by rememberUpdatedState(onSwipeLeft)
    val currentRight by rememberUpdatedState(onSwipeRight)
    val threshold = with(LocalDensity.current) { 52.dp.toPx() }

    pointerInput(currentLeft != null, currentRight != null, threshold) {
        var dragDistance = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragDistance = 0f },
            onHorizontalDrag = { change, amount ->
                dragDistance += amount
                change.consume()
            },
            onDragCancel = { dragDistance = 0f },
            onDragEnd = {
                when {
                    dragDistance <= -threshold -> currentLeft?.invoke()
                    dragDistance >= threshold -> currentRight?.invoke()
                }
                dragDistance = 0f
            },
        )
    }
}
