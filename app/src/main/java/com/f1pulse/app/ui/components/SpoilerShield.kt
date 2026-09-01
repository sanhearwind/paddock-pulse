package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

/**
 * Overlay that hides [content] until the user taps to reveal (spoiler protection for
 * race results). When [enabled] is false the content shows directly.
 */
@Composable
fun SpoilerShield(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var revealed by rememberSaveable { mutableStateOf(!enabled) }
    Box(modifier) {
        content()
        if (enabled && !revealed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { revealed = true }
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "点击查看结果 / Tap to reveal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}
