package com.f1pulse.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Color palette for F1WidgetText. Returns ARGB Int values for direct use
 * with Canvas/Paint in WidgetTextRenderer.
 *
 * These mirror the F1WidgetColors ColorProviders but resolve to concrete
 * colors based on [isLight] (detected in provideGlance via Configuration).
 */
object F1WidgetPalette {
    fun primary() = 0xFFE10600.toInt()
    fun onPrimary() = 0xFFFFFFFF.toInt()
    fun textPrimary(isLight: Boolean) =
        if (isLight) 0xFF1A1A1E.toInt() else 0xFFEAEAEF.toInt()
    fun textSecondary(isLight: Boolean) =
        if (isLight) 0xFF6B6B73.toInt() else 0xFF8E8E96.toInt()
    fun textMuted(isLight: Boolean) =
        if (isLight) 0xFF767680.toInt() else 0xFF8E8E96.toInt()
    fun pos1() = 0xFFE10600.toInt()
    fun pos2(isLight: Boolean) =
        if (isLight) 0xFF8A8A92.toInt() else 0xFFC0C0C0.toInt()
    fun pos3(isLight: Boolean) =
        if (isLight) 0xFF9A5B35.toInt() else 0xFFCD7F32.toInt()
    fun teamColor(hex: String?): Int = TeamPalette.forHex(hex).toArgb()

    /** Position badge color: 1st=red, 2nd=silver, 3rd=bronze, else=muted. */
    fun positionColor(pos: Int, isLight: Boolean) = when (pos) {
        1 -> pos1()
        2 -> pos2(isLight)
        3 -> pos3(isLight)
        else -> textMuted(isLight)
    }
}

/**
 * Renders text using the F1 brand font as a transparent bitmap Image.
 *
 * Use this instead of Text() in Glance widgets to display text in the real
 * F1 typeface. The text is rendered via Canvas.drawText with the actual
 * formula1_bold/regular/black resources loaded from res/font.
 *
 * Characters not in the F1 font (Chinese, emoji, symbols) automatically
 * fall back to the system font via Android's font fallback mechanism.
 *
 * @param text     The text to render.
 * @param color    ARGB color Int — use F1WidgetPalette helpers.
 * @param fontSize Base font size in sp (will be scaled by adaptiveSp).
 * @param isLight  Whether the system is in light mode (for color resolution).
 * @param font     "bold", "regular", "black", or "system" for non-brand glyphs.
 * @param modifier GlanceModifier — do NOT include width() or height()
 *                 (F1WidgetText sets its own size from the bitmap).
 * @param maxWidthDp  When > 0, auto-fit font size so the bitmap width ≤ this.
 *                    The text is shown in full (no truncation), just smaller.
 * @param maxHeightDp When > 0, auto-fit font size so the bitmap height ≤ this.
 */
@Composable
fun F1WidgetText(
    text: String,
    color: Int,
    fontSize: Float,
    isLight: Boolean,
    font: String = "bold",
    modifier: GlanceModifier = GlanceModifier,
    maxWidthDp: Float = 0f,
    maxHeightDp: Float = 0f,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val scaledSp = adaptiveSp(fontSize).value

    val bitmap = if (maxWidthDp > 0f || maxHeightDp > 0f) {
        WidgetTextRenderer.renderFit(
            context = context,
            text = text,
            font = font,
            color = color,
            sizeSp = scaledSp,
            maxWidthDp = if (maxWidthDp > 0f) maxWidthDp else Float.MAX_VALUE,
            maxHeightDp = if (maxHeightDp > 0f) maxHeightDp else Float.MAX_VALUE,
        )
    } else {
        WidgetTextRenderer.render(context, text, font, color, scaledSp)
    }

    // Convert bitmap pixel dimensions back to dp for Glance layout.
    // Rendered at device density (sharp), displayed at correct dp size.
    val widthDp = (bitmap.width / density).toInt().coerceAtLeast(1)
    val heightDp = (bitmap.height / density).toInt().coerceAtLeast(1)

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = text,
        modifier = modifier.width(widthDp.dp).height(heightDp.dp),
    )
}
