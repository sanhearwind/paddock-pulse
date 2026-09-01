package com.f1pulse.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.f1pulse.app.ui.theme.F1Grey
import com.f1pulse.app.ui.theme.F1Green
import com.f1pulse.app.ui.theme.F1Red

/**
 * Position change indicator. Positive [delta] = moved up (green ▲),
 * negative = moved down (red ▼), zero = unchanged (grey –).
 */
@Composable
fun PositionDelta(
    delta: Int?,
    modifier: Modifier = Modifier,
) {
    if (delta == null) {
        Spacer(modifier)
        return
    }
    val (color, arrow) = when {
        delta > 0 -> F1Green to "▲"
        delta < 0 -> F1Red to "▼"
        else -> F1Grey to "–"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(arrow, color = color, style = MaterialTheme.typography.labelMedium)
        if (delta != 0) {
            Spacer(Modifier.width(2.dp))
            Text(kotlin.math.abs(delta).toString(), color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Suppress("unused")
private val DeltaAccent: Color = F1Green
