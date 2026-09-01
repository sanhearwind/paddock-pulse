package com.f1pulse.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.f1pulse.app.core.TeamColors
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps constructor ids / hex strings to Compose [Color]s for team-themed UI accents.
 * Hex strings are stored WITHOUT leading `#` (matching OpenF1's `team_colour`).
 *
 * Parsed colours are cached in a [ConcurrentHashMap] to avoid repeated
 * `String.toLong(16)` allocations on every recomposition (critical for
 * list items that call [forHex] multiple times per row).
 */
object TeamPalette {

    private val hexCache = ConcurrentHashMap<String, Color>()
    private val constructorCache = ConcurrentHashMap<String, Color>()

    fun forConstructor(id: String?): Color {
        if (id.isNullOrBlank()) return F1Red
        return constructorCache.getOrPut(id) {
            val hex = TeamColors.hexFor(id)
            hex?.let { parseHex(it) } ?: F1Red
        }
    }

    fun forHex(hex: String?): Color {
        if (hex.isNullOrBlank()) return F1Red
        val key = hex.removePrefix("#")
        return hexCache.getOrPut(key) { parseHex(key) }
    }

    private fun parseHex(hex: String): Color = runCatching {
        Color(("FF" + hex.removePrefix("#")).toLong(16))
    }.getOrDefault(F1Red)
}
