package com.f1pulse.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal fun adaptiveBackIconColor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() >= 0.55f) Color.Black else Color.White

@Composable
fun AdaptiveBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    val iconColor = adaptiveBackIconColor(backgroundColor)
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = iconColor,
        )
    }
}
