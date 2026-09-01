package com.f1pulse.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.f1pulse.app.core.RaceCardAssets

/**
 * Race card image. Loads `assets/race_cards/{slug}.webp` for race detail
 * headers and calendar thumbnails. Caller should overlay a gradient for
 * text readability.
 */
@Composable
fun RaceCardImage(
    circuitId: String?,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val asset = remember(circuitId) { RaceCardAssets.cardAsset(circuitId) }

    if (asset.isNullOrBlank()) return

    val request = remember(asset) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$asset")
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}
