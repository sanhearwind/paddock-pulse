package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Circular driver headshot loaded via Coil. Tries the network URL first,
 * then falls back to a bundled asset under `driver_headshots/`, and
 * finally shows the driver's code on a team-coloured disc.
 *
 * @param useSubcompose Set to `true` for hero/detail screens that need
 *   custom loading/error composables. Defaults to `false` for list items
 *   where [AsyncImage] is significantly more performant.
 */
@Composable
fun DriverHeadshot(
    url: String?,
    code: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    teamHex: String? = null,
    useSubcompose: Boolean = false,
    useSquareAsset: Boolean = false,
) {
    val context = LocalContext.current
    val shape = CircleShape

    val localModel = remember(code, useSquareAsset) {
        if (useSquareAsset) driverHeadshotSquareAsset(code) else driverHeadshotAsset(code)
    }
    val model = url?.takeIf { it.isNotBlank() } ?: localModel

    if (model.isNullOrBlank()) {
        PlaceholderBox(code, modifier, size, shape, teamHex)
    } else {
        val request = remember(model) {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }

        if (useSubcompose) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = code,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = modifier.size(size).clip(shape),
                loading = {
                    Box(Modifier.fillMaxSize().background(shimmerBrush()))
                },
                error = {
                    if (!url.isNullOrBlank() && localModel != null) {
                        AsyncImage(
                            model = localModel,
                            contentDescription = code,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PlaceholderBox(code, Modifier.fillMaxSize(), size, shape, teamHex)
                    }
                },
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = code,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        // Keep the hair inside the circular top while clipping
                        // slightly more neck at the bottom.
                        .offset(y = if (useSquareAsset) 2.dp else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceholderBox(
    code: String?,
    modifier: Modifier,
    size: Dp,
    shape: Shape,
    teamHex: String?,
) {
    val bgColor = teamHex?.let { TeamPalette.forHex(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier.size(size).clip(shape).background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            code?.take(3)?.ifBlank { "?" } ?: "?",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}
