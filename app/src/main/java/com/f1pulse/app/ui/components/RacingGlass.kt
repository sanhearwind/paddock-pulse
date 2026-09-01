package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.f1pulse.app.ui.theme.F1Red

/**
 * Lightweight "Racing Glass" material. It uses translucent gradients and
 * highlights instead of blurring the composable itself, so API 26-30 devices
 * keep the same hierarchy without paying for a fake background blur.
 */
@Composable
fun RacingGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fillAlphaMultiplier: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fill = if (dark) {
        listOf(
            Color.White.copy(alpha = 0.18f),
            F1Red.copy(alpha = 0.055f),
            Color.White.copy(alpha = 0.065f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.38f),
            F1Red.copy(alpha = 0.035f),
            Color.White.copy(alpha = 0.16f),
        )
    }
    val border = if (dark) {
        listOf(Color.White.copy(alpha = 0.30f), F1Red.copy(alpha = 0.18f), Color.White.copy(alpha = 0.08f))
    } else {
        listOf(Color.White.copy(alpha = 0.98f), F1Red.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.08f))
    }
    val effectiveFill = fill.map { color ->
        color.copy(alpha = color.alpha * fillAlphaMultiplier.coerceIn(0f, 1f))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (dark) 0.30f else 0.10f),
                spotColor = F1Red.copy(alpha = 0.10f),
            )
            .clip(shape)
            .background(Brush.linearGradient(effectiveFill))
            .border(1.dp, Brush.linearGradient(border), shape)
            .padding(contentPadding),
        content = content,
    )
}

/** Page backdrop that gives translucent surfaces visible depth and red energy. */
@Composable
fun RacingGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF08090D), Color(0xFF18151C), Color(0xFF08090D)),
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White, Color.White),
        )
    }

    Box(
        modifier = modifier
            .background(base)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            F1Red.copy(alpha = if (dark) 0.34f else 0f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.minDimension * 0.92f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4D7CFE).copy(alpha = if (dark) 0.20f else 0f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.68f),
                        radius = size.minDimension * 0.78f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            F1Red.copy(alpha = if (dark) 0.18f else 0f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.74f, size.height * 0.72f),
                        radius = size.minDimension * 0.62f,
                    ),
                )
                val stripeColor = if (dark) {
                    Color.White.copy(alpha = 0.055f)
                } else {
                    Color.Transparent
                }
                val stripeWidth = size.width * 0.12f
                repeat(4) { index ->
                    val x = size.width * (-0.35f + index * 0.42f)
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, size.height),
                        end = Offset(x + size.width * 0.72f, 0f),
                        strokeWidth = stripeWidth,
                    )
                }
            },
        content = content,
    )
}
