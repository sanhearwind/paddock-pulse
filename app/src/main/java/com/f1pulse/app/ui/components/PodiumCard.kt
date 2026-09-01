package com.f1pulse.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.f1pulse.app.domain.model.RaceResult
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * 领奖台卡片：用车手半身像替代柱状图。
 * 中间第一名最大，两侧第二、第三名依次缩小，名字显示在头像下方。
 */
@Composable
fun PodiumCard(
    p1: RaceResult?,
    p2: RaceResult?,
    p3: RaceResult?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumHeadshot(result = p2, position = 2, headshotSize = 76.dp, modifier = Modifier.weight(1f))
        PodiumHeadshot(result = p1, position = 1, headshotSize = 104.dp, modifier = Modifier.weight(1f))
        PodiumHeadshot(result = p3, position = 3, headshotSize = 64.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumHeadshot(
    result: RaceResult?,
    position: Int,
    headshotSize: Dp,
    modifier: Modifier = Modifier,
) {
    val teamColor = TeamPalette.forHex(result?.team?.colourHex)
    // 入场缩放动画，按名次延迟
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, delayMillis = position * 80),
        label = "headshotScale$position",
    )

    val ringColor = when (position) {
        1 -> F1Red
        else -> teamColor
    }
    val ringWidth = when (position) {
        1 -> 3.dp
        else -> 2.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            // 头像 + 车队色圆环 + 位置徽章
            Box(
                modifier = Modifier
                    .size(headshotSize * scale)
                    .clip(CircleShape)
                    .background(teamColor.copy(alpha = 0.15f))
                    .border(ringWidth, ringColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (result != null) {
                    DriverHeadshot(
                        url = result.driver.headshotUrl,
                        code = result.driver.code,
                        size = headshotSize * scale - (ringWidth * 2),
                        teamHex = result.team.colourHex,
                        useSubcompose = position == 1,
                    )
                } else {
                    Text(
                        "-",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 位置数字徽章（底部叠加）
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 6.dp)
                    .clip(CircleShape)
                    .background(ringColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    position.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 车手代号
        Text(
            result?.driver?.code ?: "-",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = teamColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        // 车手姓氏
        Text(
            result?.driver?.familyName ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
