package com.minimal.tvlauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import java.util.ArrayDeque

/**
 * On-device background removal API.
 * Automatically strips solid background shapes/squircle cards from application icons
 * so only the clean logo emblem sits directly on the dark gray TV buttons.
 */
object IconBackgroundRemover {

    fun processIcon(context: Context, drawable: Drawable): Drawable {
        // 1. For AdaptiveIconDrawable (Android 8.0+), extract foreground layer directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val foreground = drawable.foreground
            if (foreground != null) {
                val fgBitmap = drawableToBitmap(foreground, 128, 128)
                val transparentBitmap = removeBorderBackground(fgBitmap)
                return BitmapDrawable(context.resources, transparentBitmap)
            }
        }

        // 2. Convert standard drawable to bitmap
        val originalBitmap = drawableToBitmap(drawable, 128, 128)

        // 3. Strip solid outer background (e.g. solid white/gray card borders)
        val transparentBitmap = removeBorderBackground(originalBitmap)
        return BitmapDrawable(context.resources, transparentBitmap)
    }

    private fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Fast BFS flood-fill background remover.
     * Identifies border background color and makes contiguous boundary pixels transparent.
     */
    private fun removeBorderBackground(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = source.copy(Bitmap.Config.ARGB_8888, true)

        // Sample 4 corner pixels
        val c0 = result.getPixel(0, 0)
        val c1 = result.getPixel(width - 1, 0)
        val c2 = result.getPixel(0, height - 1)
        val c3 = result.getPixel(width - 1, height - 1)

        // If corners are already transparent, no solid background removal needed
        val cornerAlphaAvg = (Color.alpha(c0) + Color.alpha(c1) + Color.alpha(c2) + Color.alpha(c3)) / 4
        if (cornerAlphaAvg < 30) {
            return result
        }

        val bgR = Color.red(c0)
        val bgG = Color.green(c0)
        val bgB = Color.blue(c0)

        val visited = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()

        // Seed boundary pixels
        for (x in 0 until width) {
            queue.add(x)
            queue.add((height - 1) * width + x)
        }
        for (y in 1 until height - 1) {
            queue.add(y * width)
            queue.add(y * width + (width - 1))
        }

        // Color distance tolerance squared (threshold for background matching)
        val toleranceSq = 42 * 42 * 3

        while (!queue.isEmpty()) {
            val idx = queue.poll() ?: break
            if (visited[idx]) continue
            visited[idx] = true

            val x = idx % width
            val y = idx / width
            val px = result.getPixel(x, y)

            val dr = Color.red(px) - bgR
            val dg = Color.green(px) - bgG
            val db = Color.blue(px) - bgB
            val distSq = dr * dr + dg * dg + db * db

            if (distSq < toleranceSq && Color.alpha(px) > 20) {
                result.setPixel(x, y, Color.TRANSPARENT)

                // Enqueue 4-way neighbors
                if (x > 0 && !visited[idx - 1]) queue.add(idx - 1)
                if (x < width - 1 && !visited[idx + 1]) queue.add(idx + 1)
                if (y > 0 && !visited[idx - width]) queue.add(idx - width)
                if (y < height - 1 && !visited[idx + width]) queue.add(idx + width)
            }
        }

        return result
    }
}
