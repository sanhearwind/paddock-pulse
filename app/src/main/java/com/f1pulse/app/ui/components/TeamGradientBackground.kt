package com.f1pulse.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.f1pulse.app.core.TeamAccessibleColors
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * 车队色渐变背景修饰符。模拟 F1 官网车手卡片背景：
 * 左侧 accessible-colour 不透明 → 右侧透明（深色模式）或 surface 色（浅色模式）。
 * v1.4 新增，用于车手详情英雄区。
 *
 * 用法：`Modifier.teamGradient(driver.team?.constructorId)`
 */
@Composable
fun Modifier.teamGradient(
    constructorId: String?,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accessibleHex = TeamAccessibleColors.hexFor(constructorId)
    val accessible = accessibleHex?.let { TeamPalette.forHex(it) } ?: TeamPalette.forHex(null)
    val endColor = if (isDark) Color.Transparent else MaterialTheme.colorScheme.background

    return this.then(
        Modifier.drawBehind {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(accessible, endColor),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                )
            )
        }
    )
}

/** 简单亮度计算。 */
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
