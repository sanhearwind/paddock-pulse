package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.f1pulse.app.core.TeamAssets
import com.f1pulse.app.ui.theme.TeamPalette

/** Pre-built inversion matrix for light-mode logo rendering. */
private val InvertColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

/**
 * Circular team logo loaded from bundled `assets/team_logos` WebP files via Coil.
 * Falls back to a team-coloured disc with the constructor id initial when no
 * logo is available.
 *
 * Logos are white-on-transparent (designed for dark backgrounds). In dark mode
 * they render as-is. In light mode a ColorMatrix inversion filter is applied
 * so the white logo becomes black and stays visible without any background fill.
 */
@Composable
fun TeamLogo(
    constructorId: String?,
    colourHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val asset = remember(constructorId) { TeamAssets.logoAsset(constructorId) }
    val shape = CircleShape

    if (!asset.isNullOrBlank()) {
        val isDark = isSystemInDarkTheme()
        val colorFilter = remember(isDark) {
            if (!isDark) ColorFilter.colorMatrix(InvertColorMatrix) else null
        }
        val request = remember(asset) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/$asset")
                .crossfade(true)
                .build()
        }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = request,
                contentDescription = constructorId,
                imageLoader = context.imageLoader,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier.size(size),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(colourHex?.let { TeamPalette.forHex(it) } ?: MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                constructorId?.take(1)?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}
