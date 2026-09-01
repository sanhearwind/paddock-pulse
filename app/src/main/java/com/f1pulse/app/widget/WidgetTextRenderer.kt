package com.f1pulse.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import kotlin.math.min

/** Renders widget text with portable system typefaces. */
object WidgetTextRenderer {
    private const val CACHE_BYTES = 6 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private fun getTypeface(font: String): Typeface = when (font) {
        "regular", "system" -> Typeface.DEFAULT
        else -> Typeface.DEFAULT_BOLD
    }

    fun render(
        context: Context,
        text: String,
        font: String,
        color: Int,
        sizeSp: Float,
    ): Bitmap {
        if (text.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val density = context.resources.displayMetrics.density
        val sizePx = sizeSp * density
        val key = "$text|$font|$color|${sizePx.toInt()}"
        cache.get(key)?.let { return it }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getTypeface(font)
            textSize = sizePx
            this.color = color
        }
        val metrics = paint.fontMetrics
        val width = (paint.measureText(text) + 0.5f).toInt().coerceAtLeast(1)
        val height = (metrics.descent - metrics.ascent + 0.5f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 0f, -metrics.ascent, paint)
        cache.put(key, bitmap)
        return bitmap
    }

    fun renderFit(
        context: Context,
        text: String,
        font: String,
        color: Int,
        sizeSp: Float,
        maxWidthDp: Float,
        maxHeightDp: Float,
    ): Bitmap {
        if (text.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val density = context.resources.displayMetrics.density
        val maxWidthPx = maxWidthDp * density
        val maxHeightPx = maxHeightDp * density
        val fitKey = "fit|$text|$font|$color|${sizeSp.toInt()}|${maxWidthPx.toInt()}|${maxHeightPx.toInt()}"
        cache.get(fitKey)?.let { return it }

        val full = render(context, text, font, color, sizeSp)
        if (full.width <= maxWidthPx && full.height <= maxHeightPx) {
            cache.put(fitKey, full)
            return full
        }

        var low = 6f
        var high = sizeSp
        var best = full
        repeat(13) {
            if (high - low < 0.5f) return@repeat
            val middle = (low + high) / 2f
            val candidate = render(context, text, font, color, middle)
            if (candidate.width <= maxWidthPx && candidate.height <= maxHeightPx) {
                best = candidate
                low = middle
            } else {
                high = middle
            }
        }
        val bounded = scaleToBounds(best, maxWidthPx, maxHeightPx)
        cache.put(fitKey, bounded)
        return bounded
    }

    private fun scaleToBounds(bitmap: Bitmap, maxWidthPx: Float, maxHeightPx: Float): Bitmap {
        val widthLimit = if (maxWidthPx > 0f) maxWidthPx else Float.MAX_VALUE
        val heightLimit = if (maxHeightPx > 0f) maxHeightPx else Float.MAX_VALUE
        val scale = min(widthLimit / bitmap.width, heightLimit / bitmap.height)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}
