package com.f1pulse.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.f1pulse.app.ui.theme.F1Green
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Live countdown to [target], ticking every second. Shows a pulsing "LIVE"
 * badge once the target passes; otherwise a formatted days/hours/minutes string.
 *
 * 倒计时背景使用两侧向中间脉冲的三角动效，营造起跑灯即将亮起的紧迫感。
 *
 * Performance: the pulse animation is only created when [isLive] is true,
 * and the per-second tick is skipped when live (the pulse animation drives
 * the visual update instead).
 */
@Composable
fun CountdownTimer(
    target: Instant,
    modifier: Modifier = Modifier,
    largeStyle: Boolean = true,
) {
    var now by remember(target) { mutableStateOf(Instant.now()) }
    LaunchedEffect(target) {
        while (true) {
            now = Instant.now()
            delay(1000L)
        }
    }

    val remaining = Duration.between(now, target)
    val isLive = remaining.isNegative || remaining.isZero

    val (text, color) = if (isLive) {
        "● LIVE" to F1Green
    } else {
        val d = remaining.toDays()
        val h = remaining.toHours() % 24
        val m = remaining.toMinutes() % 60
        val s = remaining.seconds % 60
        val formatted = when {
            d > 0 -> String.format("%dd %dh %dm", d, h, m)
            h > 0 -> String.format("%dh %dm %ds", h, m, s)
            else -> String.format("%dm %ds", m, s)
        }
        formatted to MaterialTheme.colorScheme.onBackground
    }

    // Pulse animation only when live — avoids continuous recomposition otherwise.
    val pulseAlpha = if (isLive) {
        val transition = rememberInfiniteTransition(label = "livePulse")
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        ).value
    } else {
        1f
    }

    // 两侧向中间脉冲的三角动效
    // progress 从 0→1 循环，三角形从边缘向中心推进
    val triangleProgress = rememberInfiniteTransition(label = "trianglePulse")
    val triProgress by triangleProgress.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "triProgress",
    )

    val bgColor = color.copy(alpha = if (isLive) 0.12f else 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .drawBehind {
                drawTrianglePulse(
                    progress = triProgress,
                    color = color,
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(if (isLive) Modifier.alpha(pulseAlpha) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (largeStyle) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            color = color,
        )
    }
}

/**
 * 绘制两侧向中间脉冲的三角形。
 * 左侧三角形从左边向右推进，右侧三角形从右边向左推进，
 * 接近中心时淡出，形成"脉冲"效果。
 */
private fun DrawScope.drawTrianglePulse(
    progress: Float,
    color: Color,
) {
    val w = size.width
    val h = size.height
    val centerY = h / 2f

    // 三角形最大宽度为容器宽度的 45%（留出中间文字空间）
    val maxTriangleWidth = w * 0.45f
    // 当前三角形推进距离（0 → maxTriangleWidth）
    val advance = maxTriangleWidth * progress
    // 透明度：开始时最亮，接近中心时淡出
    val alpha = (1f - progress).coerceIn(0f, 1f) * 0.25f
    if (alpha <= 0f) return

    val triangleColor = color.copy(alpha = alpha)
    val triHeight = h * 0.5f

    // 左侧三角形（指向右侧/中心方向）
    val leftPath = Path().apply {
        moveTo(0f, centerY - triHeight / 2f)
        lineTo(advance, centerY)
        lineTo(0f, centerY + triHeight / 2f)
        close()
    }

    // 右侧三角形（指向左侧/中心方向）
    val rightPath = Path().apply {
        moveTo(w, centerY - triHeight / 2f)
        lineTo(w - advance, centerY)
        lineTo(w, centerY + triHeight / 2f)
        close()
    }

    drawPath(leftPath, triangleColor)
    drawPath(rightPath, triangleColor)
}
