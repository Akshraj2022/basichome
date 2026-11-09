package com.minimal.tvlauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.minimal.tvlauncher.R
import java.util.Locale

/**
 * 100% Offline, high-definition squircle icon engine for Android TV.
 *
 * 1. Zero external network/API dependencies - completely self-sufficient and offline.
 * 2. Extracts high-density authentic icons directly from installed APK packages via PackageManager.
 * 3. Supports both adaptive square icons and wide 16:9 Leanback TV banners (center-cropped to 256x256).
 * 4. Applies hardware-accelerated anti-aliased 18% squircle corner curvature.
 * 5. Deterministic typographic monogram fallback with elegant dark squircle styling (no weird hollow boxes).
 */
object IconCache {

    private const val MAX_CACHE_SIZE = 128
    private val cache = object : LruCache<String, Drawable>(MAX_CACHE_SIZE) {}

    fun clear() {
        cache.evictAll()
    }

    fun getIcon(
        context: Context,
        packageName: String,
        appName: String = "",
        onAsyncLoaded: ((Drawable) -> Unit)? = null
    ): Drawable {
        val cached = cache.get(packageName)
        if (cached != null) return cached

        // 1. Settings App special case (sleek settings gear icon)
        if (packageName.contains("settings", ignoreCase = true)) {
            val settingsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_settings)
            if (settingsDrawable != null) {
                val curved = createCurvedDrawable(context, drawableToBitmap(settingsDrawable, 256, 256))
                cache.put(packageName, curved)
                return curved
            }
        }

        // 2. High-res APK icon extraction
        val pm = context.packageManager
        var rawDrawable: Drawable? = null

        try {
            val launchIntent = pm.getLeanbackLaunchIntentForPackage(packageName)
                ?: pm.getLaunchIntentForPackage(packageName)
            val activityInfo = launchIntent?.resolveActivityInfo(pm, 0)
            val appInfo = try { pm.getApplicationInfo(packageName, 0) } catch (_: Exception) { null }

            // A. High-density resource retrieval (XXXHIGH 640dpi -> XXHIGH 480dpi)
            if (appInfo != null) {
                val appRes = try { pm.getResourcesForApplication(appInfo) } catch (_: Exception) { null }
                if (appRes != null) {
                    val iconResId = if (activityInfo != null && activityInfo.icon != 0) {
                        activityInfo.icon
                    } else if (appInfo.icon != 0) {
                        appInfo.icon
                    } else 0

                    if (iconResId != 0) {
                        rawDrawable = try {
                            appRes.getDrawableForDensity(iconResId, DisplayMetrics.DENSITY_XXXHIGH, null)
                        } catch (_: Exception) {
                            try {
                                appRes.getDrawableForDensity(iconResId, DisplayMetrics.DENSITY_XXHIGH, null)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
            }

            // B. Direct Activity or Application icon
            if (rawDrawable == null && activityInfo != null) {
                rawDrawable = try { activityInfo.loadIcon(pm) } catch (_: Exception) { null }
            }
            if (rawDrawable == null && appInfo != null) {
                rawDrawable = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
            }
            if (rawDrawable == null) {
                rawDrawable = try { pm.getApplicationIcon(packageName) } catch (_: Exception) { null }
            }

            // C. Android TV Leanback Banner fallback
            if (rawDrawable == null && activityInfo != null) {
                rawDrawable = try { activityInfo.loadBanner(pm) } catch (_: Exception) { null }
            }
            if (rawDrawable == null && appInfo != null) {
                rawDrawable = try { appInfo.loadBanner(pm) } catch (_: Exception) { null }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Elegant modern typography monogram (never null, never hollow boxes)
        val finalBitmap = if (rawDrawable != null) {
            drawableToBitmap(rawDrawable, 256, 256)
        } else {
            createMonogramBitmap(appName.ifEmpty { packageName })
        }

        val curved = createCurvedDrawable(context, finalBitmap)
        cache.put(packageName, curved)
        return curved
    }

    private fun createCurvedDrawable(context: Context, bitmap: Bitmap): Drawable {
        val rounded = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
        rounded.cornerRadius = bitmap.width * 0.18f
        rounded.setAntiAlias(true)
        return rounded
    }

    private fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            val srcW = b.width
            val srcH = b.height
            if (srcW > 0 && srcH > 0) {
                val aspect = srcW.toFloat() / srcH.toFloat()
                if (aspect > 1.25f) {
                    // Center crop wide 16:9 banner into 1:1 squircle
                    val cropSize = srcH
                    val cropX = (srcW - cropSize) / 2
                    val cropped = Bitmap.createBitmap(b, cropX, 0, cropSize, cropSize)
                    return Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
                } else if (aspect < 0.8f) {
                    val cropSize = srcW
                    val cropY = (srcH - cropSize) / 2
                    val cropped = Bitmap.createBitmap(b, 0, cropY, cropSize, cropSize)
                    return Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
                } else {
                    if (srcW == targetWidth && srcH == targetHeight) return b
                    return Bitmap.createScaledBitmap(b, targetWidth, targetHeight, true)
                }
            }
        }

        val intrinsicW = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetWidth
        val intrinsicH = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetHeight

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val aspect = intrinsicW.toFloat() / intrinsicH.toFloat()
        if (aspect > 1.25f) {
            // Wide banner
            val scaledW = (targetHeight * aspect).toInt()
            val left = (targetWidth - scaledW) / 2
            drawable.setBounds(left, 0, left + scaledW, targetHeight)
        } else if (aspect < 0.8f) {
            val scaledH = (targetWidth / aspect).toInt()
            val top = (targetHeight - scaledH) / 2
            drawable.setBounds(0, top, targetWidth, top + scaledH)
        } else {
            drawable.setBounds(0, 0, targetWidth, targetHeight)
        }

        drawable.draw(canvas)
        return bitmap
    }

    private fun createMonogramBitmap(name: String): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val hash = kotlin.math.abs(name.hashCode())
        val palette = intArrayOf(
            0xFF1E2028.toInt(),
            0xFF23252E.toInt(),
            0xFF202630.toInt(),
            0xFF26222C.toInt(),
            0xFF202A26.toInt(),
            0xFF292420.toInt()
        )
        val bgColor = palette[hash % palette.size]

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val cleanName = name.trim().uppercase(Locale.getDefault())
        val letter = if (cleanName.isNotEmpty()) {
            cleanName.firstOrNull { it.isLetterOrDigit() }?.toString() ?: cleanName.take(1)
        } else "A"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 104f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bitmap
    }
}
