package com.f1pulse.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Car number image. Loads `assets/car_numbers/{driverCode}.webp` (white silhouette)
 * and tints it with the team colour via [ColorFilter.tint].
 */
@Composable
fun CarNumber(
    driverCode: String?,
    teamHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    if (driverCode.isNullOrBlank()) return

    val context = LocalContext.current
    val teamColor = remember(teamHex) { TeamPalette.forHex(teamHex) }
    val colorFilter = remember(teamColor) { ColorFilter.tint(teamColor, BlendMode.SrcIn) }
    val request = remember(driverCode) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/car_numbers/${driverCode.lowercase()}.webp")
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = driverCode,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = modifier.size(size),
    )
}
