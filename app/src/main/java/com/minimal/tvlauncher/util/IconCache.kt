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
 * High-definition Play Store curved icon cache.
 * 1. Serves official 512x512 Google Play Store icons scaled via Google's free CDN scaler.
 * 2. Applies hardware-accelerated anti-aliased curved squircle corners.
 * 3. Falls back immediately to high-density APK icons (XXXHIGH/XXHIGH) with zero waiting.
 * 4. Guarantees no icon is ever blank or low-resolution.
 */
object IconCache {

    private const val MAX_CACHE_SIZE = 96
    private val cache = object : LruCache<String, Drawable>(MAX_CACHE_SIZE) {}

    fun getIcon(
        context: Context,
        packageName: String,
        appName: String = "",
        onAsyncLoaded: ((Drawable) -> Unit)? = null
    ): Drawable {
        val cached = cache.get(packageName)
        if (cached != null) return cached

        // 1. Custom matching squircle icon for TV Settings
        if (packageName.contains("settings")) {
            val settingsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_settings)
            if (settingsDrawable != null) {
                val curved = createCurvedDrawable(context, drawableToBitmap(settingsDrawable, 256, 256))
                cache.put(packageName, curved)
                return curved
            }
        }

        // 2. Check persistent storage for previously downloaded 512x512 Play Store icon
        val storedBitmap = PlayStoreIconHelper.loadStoredIcon(context, packageName)
        if (storedBitmap != null) {
            val rounded = createCurvedDrawable(context, storedBitmap)
            cache.put(packageName, rounded)
            return rounded
        }

        // 2. Trigger asynchronous background fetch from Google Play CDN scaler
        PlayStoreIconHelper.fetchPlayStoreIconAsync(context, packageName) { freshBitmap ->
            val freshRounded = createCurvedDrawable(context, freshBitmap)
            cache.put(packageName, freshRounded)
            onAsyncLoaded?.invoke(freshRounded)
        }

        // 3. Immediate local fallback: Extract high-density (640dpi XXXHIGH) icon from APK
        val pm = context.packageManager
        var rawDrawable: Drawable? = null
        try {
            val launchIntent = pm.getLeanbackLaunchIntentForPackage(packageName)
                ?: pm.getLaunchIntentForPackage(packageName)
            val activityInfo = launchIntent?.resolveActivityInfo(pm, 0)
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appRes = pm.getResourcesForApplication(appInfo)

            val iconResId = if (activityInfo != null && activityInfo.icon != 0) {
                activityInfo.icon
            } else if (appInfo.icon != 0) {
                appInfo.icon
            } else {
                0
            }

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

            if (rawDrawable == null) {
                rawDrawable = activityInfo?.loadIcon(pm) ?: appInfo.loadIcon(pm)
            }
        } catch (_: Exception) {}

        // 4. Vector asset fallback for system & media items
        if (rawDrawable == null) {
            val brandResId = when {
                packageName.contains("netflix") -> R.drawable.logo_netflix
                packageName.contains("youtube") && !packageName.contains("music") -> R.drawable.logo_youtube
                packageName.contains("amazonvideo") || packageName.contains("prime") -> R.drawable.logo_prime
                packageName.contains("crunchyroll") -> R.drawable.logo_crunchyroll
                packageName.contains("disney") || packageName.contains("hotstar") -> R.drawable.logo_disney
                packageName.contains("katniss") || packageName.contains("googlequicksearchbox") -> R.drawable.logo_google
                packageName.contains("settings") -> R.drawable.ic_settings
                packageName.contains("ludo") || packageName.contains("game") -> R.drawable.ic_gamepad
                else -> null
            }
            if (brandResId != null) {
                rawDrawable = ContextCompat.getDrawable(context, brandResId)
            }
        }

        // 5. Stylized monogram fallback to guarantee no icon is ever null
        val finalDrawable = rawDrawable ?: createMonogramDrawable(context, appName.ifEmpty { packageName })
        val curved = createCurvedDrawable(context, drawableToBitmap(finalDrawable, 256, 256))
        cache.put(packageName, curved)
        return curved
    }

    private fun createCurvedDrawable(context: Context, bitmap: Bitmap): Drawable {
        val rounded = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
        rounded.cornerRadius = bitmap.width * 0.18f
        rounded.setAntiAlias(true)
        return rounded
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            if (b.width >= width && b.height >= height) return b
            return Bitmap.createScaledBitmap(b, width, height, true)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createMonogramDrawable(context: Context, name: String): Drawable {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222228")
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val text = name.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "?" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 110f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val yPos = (size / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, size / 2f, yPos, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}
