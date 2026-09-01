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
import com.f1pulse.app.core.CarAssets

/**
 * Race car image. Loads `assets/cars/{team}.webp` (right-side 45° view)
 * for the driver detail car display section.
 */
@Composable
fun CarImage(
    constructorId: String?,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    val context = LocalContext.current
    val asset = remember(constructorId) { CarAssets.carAsset(constructorId) } ?: return

    val request = remember(asset) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$asset")
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = constructorId,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}
