package com.f1pulse.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.f1pulse.app.core.TeamAssets
import com.f1pulse.app.ui.components.driverHeadshotAsset
import com.f1pulse.app.ui.components.driverHeadshotSquareAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads small, cached asset bitmaps suitable for RemoteViews-backed widgets. */
object WidgetImageLoader {
    // Keep widget images close to their bundled resolution. A 64px logo is
    // upscaled by common launchers for a 40–48dp slot and looks soft.
    private const val HEADSHOT_PX = 128
    private const val LOGO_PX = 160
    private const val TRACK_THUMBNAIL_PX = 512

    private val cache = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun loadHeadshot(context: Context, driverCode: String?): Bitmap? {
        val assetPath = driverHeadshotSquareAsset(driverCode) ?: return null
        val cleanPath = assetPath.removePrefix("file:///android_asset/")
        val key = "headshot:$cleanPath"
        cache.get(key)?.let { return it }

        // 方形大头照已是 200x200，直接缩放到目标尺寸即可，无需裁剪
        val raw = loadAssetBitmap(context, cleanPath, HEADSHOT_PX) ?: return null
        val result = scaleTo(raw, HEADSHOT_PX)
        cache.put(key, result)
        return result
    }

    /**
     * 加载车手半身像（440x1265 竖版），按目标高度缩放后裁剪为合适的竖条。
     * 裁剪区域居中，保留车手上半身/脸部，适合 2x2 及以上自定义车手组件。
     */
    fun loadHalfBody(context: Context, driverCode: String?, targetHeightPx: Int): Bitmap? {
        val assetPath = driverHeadshotAsset(driverCode) ?: return null
        val cleanPath = assetPath.removePrefix("file:///android_asset/")
        val key = "halfbody:v2:${cleanPath}:h=$targetHeightPx"
        cache.get(key)?.let { return it }

        val raw = loadAssetBitmap(context, cleanPath, targetHeightPx) ?: return null
        // The source is a tall full-body portrait (440x1265). Crop the upper
        // body before scaling so the driver fills the right half of a square
        // widget without horizontally stretching the image.
        val targetAspect = 0.56f
        val cropHeight = (raw.width / targetAspect)
            .toInt()
            .coerceIn(1, raw.height)
        val upperBody = Bitmap.createBitmap(raw, 0, 0, raw.width, cropHeight)
        val targetWidth = (targetHeightPx * targetAspect).toInt().coerceAtLeast(1)
        val result = Bitmap.createScaledBitmap(
            upperBody,
            targetWidth,
            targetHeightPx,
            true,
        )
        cache.put(key, result)
        return result
    }

    fun loadTeamLogo(context: Context, constructorId: String?, isLightMode: Boolean): Bitmap? {
        val assetPath = TeamAssets.logoAsset(constructorId) ?: return null
        val key = "logo:$assetPath:light=$isLightMode"
        cache.get(key)?.let { return it }

        val raw = loadAssetBitmap(context, assetPath, LOGO_PX) ?: return null
        val scaled = scaleTo(raw, LOGO_PX)
        val result = if (isLightMode) invertColors(scaled) else scaled
        cache.put(key, result)
        return result
    }

    suspend fun loadTrackThumbnail(
        context: Context,
        circuitId: String?,
        isLightMode: Boolean,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val assetPath = CountdownWidgetLayoutPolicy.thumbnailAsset(circuitId)
            ?: return@withContext null
        val key = "track-thumbnail:$assetPath:light=$isLightMode"
        cache.get(key)?.let { return@withContext it }

        val request = ImageRequest.Builder(context)
            .data("file:///android_asset/$assetPath")
            .size(TRACK_THUMBNAIL_PX, TRACK_THUMBNAIL_PX)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult
            ?: return@withContext null
        val raw = result.drawable.toBitmap(
            width = result.drawable.intrinsicWidth.coerceAtLeast(1),
            height = result.drawable.intrinsicHeight.coerceAtLeast(1),
            config = Bitmap.Config.ARGB_8888,
        )
        val tint = if (isLightMode) 0xFF15151E.toInt() else 0xFFEAEAEF.toInt()
        val tinted = tintColors(raw, tint)
        cache.put(key, tinted)
        tinted
    }
    private fun scaleTo(source: Bitmap, targetPx: Int): Bitmap =
        if (source.width == targetPx && source.height == targetPx) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetPx, targetPx, true)
        }

    private fun invertColors(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val matrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        Canvas(result).drawBitmap(
            source,
            0f,
            0f,
            Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) },
        )
        return result
    }

    private fun tintColors(source: Bitmap, color: Int): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(
            source,
            0f,
            0f,
            Paint().apply {
                colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            },
        )
        return result
    }
    private fun loadAssetBitmap(context: Context, assetPath: String, targetPx: Int): Bitmap? =
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (
                bounds.outWidth / (sample * 2) >= targetPx &&
                bounds.outHeight / (sample * 2) >= targetPx
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                // Prevent BitmapFactory from applying an additional density scale.
                inScaled = false
            }
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        }.getOrNull()
}