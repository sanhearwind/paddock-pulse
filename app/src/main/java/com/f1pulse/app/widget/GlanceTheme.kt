package com.f1pulse.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.LocalSize
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontFamily

/** Public day/night providers; bundled custom fonts are intentionally not used. */
object F1WidgetColors {
    val background = ColorProvider(Color(0xFFF7F7F8), Color(0xFF0A0A0C))
    val surface = ColorProvider(Color.White, Color(0xFF121215))
    val primary = ColorProvider(Color(0xFFE10600), Color(0xFFE10600))
    val onPrimary = ColorProvider(Color.White, Color.White)
    val textPrimary = ColorProvider(Color(0xFF1A1A1E), Color(0xFFEAEAEF))
    val textSecondary = ColorProvider(Color(0xFF6B6B73), Color(0xFF8E8E96))
    val textMuted = ColorProvider(Color(0xFF767680), Color(0xFF8E8E96))
    val border = ColorProvider(Color(0xFFE2E2E6), Color(0xFF1E1E22))
    val pos1 = ColorProvider(Color(0xFFE10600), Color(0xFFE10600))
    val pos2 = ColorProvider(Color(0xFF8A8A92), Color(0xFFC0C0C0))
    val pos3 = ColorProvider(Color(0xFF9A5B35), Color(0xFFCD7F32))
}

/** Glance supports system font families only; weight supplies the F1 hierarchy. */
object F1WidgetFonts {
    val bold = FontFamily.SansSerif
    val regular = FontFamily.SansSerif
    val black = FontFamily.SansSerif
}

@Composable
fun F1WidgetTheme(content: @Composable () -> Unit) {
    content()
}

/**
 * Widget orientation classified by aspect ratio.
 * - [Landscape]: width > height × 1.2 (e.g. 4×2, 4×1)
 * - [Portrait]: height > width × 1.2 (e.g. 2×4, 1×4)
 * - [Square]: roughly equal (e.g. 2×2, 3×3, 4×4)
 *
 * Used to switch between horizontal / vertical / centered arrangements.
 */
enum class WidgetLayout { Landscape, Portrait, Square }

/**
 * Classifies the current widget's orientation from [LocalSize].
 * With [androidx.glance.appwidget.SizeMode.Exact], [LocalSize] returns the
 * real pixel dimensions the launcher assigned, so this is accurate.
 */
@Composable
fun widgetLayout(): WidgetLayout {
    val size = LocalSize.current
    val w = size.width.value
    val h = size.height.value
    val ratio = w / h
    return when {
        ratio > 1.2f -> WidgetLayout.Landscape
        ratio < 1f / 1.2f -> WidgetLayout.Portrait
        else -> WidgetLayout.Square
    }
}

/**
 * Returns a font size scaled to the widget's real dimensions.
 *
 * Typography is capped at 1.1x the baseline. Enlarging a standings widget
 * therefore creates room for more rows instead of producing oversized text.
 * Individual labels should still pass maxWidthDp/maxHeightDp when bounded.
 */
@Composable
private fun widgetScale(): Float {
    val size = LocalSize.current
    val minDim = minOf(size.width.value, size.height.value)
    // Bound typography so large widgets gain breathing room instead of clipping bitmaps.
    return (minDim / 160f).coerceIn(0.7f, 1.25f)
}

@Composable
fun adaptiveSp(base: Float): TextUnit = (base * widgetScale().coerceAtMost(1.1f)).sp

/** True for the one-row/two-column minimum widget size. */
@Composable
fun isTinyWidget(): Boolean {
    val size = LocalSize.current
    return size.width.value < 120f || size.height.value < 64f
}

/**
 * Scales a dp dimension proportionally to the widget's real size, using the
 * same baseline and bounded dimension scale. Useful for image / spacer sizes
 * that should grow with the widget.
 */
@Composable
fun adaptiveDp(base: Float): Float = base * widgetScale()
