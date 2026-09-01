package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.Formula1WideFamily
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun HomeRaceCountdown(
    target: Instant,
    modifier: Modifier = Modifier,
) {
    var now by remember(target) { mutableStateOf(Instant.now()) }
    LaunchedEffect(target) {
        while (true) {
            now = Instant.now()
            delay(1_000L)
        }
    }

    val remaining = Duration.between(now, target).coerceAtLeast(Duration.ZERO)
    val values = listOf(
        remaining.toDays() to "天",
        (remaining.toHours() % 24L) to "小时",
        (remaining.toMinutes() % 60L) to "分钟",
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground = if (dark) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(F1Red, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "开赛倒计时",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = foreground,
            )
            Text(
                "  |  LIGHTS OUT IN",
                style = MaterialTheme.typography.labelSmall,
                color = foreground.copy(alpha = 0.58f),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            values.forEachIndexed { index, (value, unit) ->
                CountdownMetric(
                    value = value,
                    unit = unit,
                    color = foreground,
                    modifier = Modifier.weight(1f),
                )
                if (index < values.lastIndex) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(foreground.copy(alpha = 0.12f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownMetric(
    value: Long,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = Formula1WideFamily,
            color = color,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.70f),
        )
    }
}
