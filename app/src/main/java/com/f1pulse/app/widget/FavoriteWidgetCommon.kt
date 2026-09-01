package com.f1pulse.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.LruCache

/** Shared helpers for the favorite team & driver widgets. */

/** Darkens an ARGB color by the given factor (0.0 = black, 1.0 = original). */
internal fun darken(color: Int, factor: Float = 0.55f): Int {
    val hsv = FloatArray(3)
    Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv)
    hsv[2] *= factor // reduce value
    return Color.HSVToColor(Color.alpha(color), hsv)
}

/** Creates a diagonal gradient bitmap from [startColor] to [darken(startColor)]. */
internal fun createGradientBitmap(startColor: Int): Bitmap {
    val w = 512
    val h = 512
    val endColor = darken(startColor, 0.45f)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val shader = LinearGradient(
        0f, 0f, w.toFloat(), h.toFloat(),
        intArrayOf(startColor, endColor),
        null,
        Shader.TileMode.CLAMP,
    )
    Canvas(bitmap).drawRect(
        0f, 0f, w.toFloat(), h.toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader },
    )
    return bitmap
}

/**
 * Caches gradient bitmaps by team color so the 512x512 (~1 MB) bitmap is only
 * allocated once per color instead of on every recomposition. F1 has a bounded
 * set of team colors, so the cache stays tiny.
 */
internal object GradientBitmapCache {
    private const val MAX_BYTES = 8 * 1024 * 1024 // ~8 gradients
    private val cache = object : LruCache<Int, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(startColor: Int): Bitmap =
        cache.get(startColor) ?: createGradientBitmap(startColor).also { cache.put(startColor, it) }
}
