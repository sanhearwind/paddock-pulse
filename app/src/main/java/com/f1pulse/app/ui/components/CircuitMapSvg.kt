package com.f1pulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Renders a bundled track map (WebP or SVG) from `assets/tracks/`. Falls back to a
 * labelled placeholder when the asset is missing so the layout never breaks.
 */
@Composable
fun CircuitMapSvg(
    svgAsset: String?,
    circuitName: String,
    modifier: Modifier = Modifier,
    height: Int = 200,
) {
    val context = LocalContext.current
    if (svgAsset.isNullOrBlank()) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                circuitName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }
    val request = remember(svgAsset) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$svgAsset")
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = circuitName,
        imageLoader = context.imageLoader,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
    )
}
