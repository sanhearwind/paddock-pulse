package com.f1pulse.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val PageEaseOut = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** A restrained directional transition for two-state page content. */
@Composable
fun HorizontalPageTransition(
    page: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    AnimatedContent(
        targetState = page,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (
                slideInHorizontally(
                    animationSpec = tween(220, easing = PageEaseOut),
                ) { width -> direction * width / 7 } +
                    fadeIn(animationSpec = tween(170))
                ).togetherWith(
                slideOutHorizontally(
                    animationSpec = tween(150, easing = PageEaseOut),
                ) { width -> -direction * width / 14 } +
                    fadeOut(animationSpec = tween(130)),
            )
        },
        label = "horizontalPageTransition",
        content = { content(it) },
    )
}
