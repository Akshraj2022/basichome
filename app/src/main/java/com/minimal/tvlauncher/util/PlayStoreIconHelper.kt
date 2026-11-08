package com.minimal.tvlauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Play Store Icon Scraper and Google Play CDN Scaler.
 * Scrapes the official Play Store listing for any Android TV package and downloads
 * the official 512x512 Ultra-HD master icon directly from Google's high-speed CDN.
 * 100% free, zero external API keys, cached permanently to device storage.
 */
object PlayStoreIconHelper {

    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val inFlight = HashSet<String>()

    private val ogImagePattern = Pattern.compile("property=\"og:image\"\\s+content=\"([^\"]+)\"")
    private val itemImagePattern = Pattern.compile("itemprop=\"image\"\\s+content=\"([^\"]+)\"")

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "playstore_icons")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getIconFile(context: Context, packageName: String): File {
        return File(getStorageDir(context), "${packageName.replace('/', '_')}.png")
    }

    fun hasStoredIcon(context: Context, packageName: String): Boolean {
        val file = getIconFile(context, packageName)
        return file.exists() && file.length() > 0
    }

    fun loadStoredIcon(context: Context, packageName: String): Bitmap? {
        val file = getIconFile(context, packageName)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Asynchronously scrapes Google Play Store for the package's 512x512 official icon,
     * stores it in persistent device storage, and notifies the caller on the main thread.
     */
    fun fetchPlayStoreIconAsync(
        context: Context,
        packageName: String,
        onLoaded: (Bitmap) -> Unit
    ) {
        if (hasStoredIcon(context, packageName)) {
            loadStoredIcon(context, packageName)?.let { onLoaded(it) }
            return
        }

        synchronized(inFlight) {
            if (inFlight.contains(packageName)) return
            inFlight.add(packageName)
        }

        executor.execute {
            try {
                val iconUrl = scrapePlayStoreIconUrl(packageName)
                if (iconUrl.isNotEmpty()) {
                    val bitmap = downloadBitmap(iconUrl)
                    if (bitmap != null) {
                        val destinationFile = getIconFile(context, packageName)
                        FileOutputStream(destinationFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        mainHandler.post {
                            onLoaded(bitmap)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                synchronized(inFlight) {
                    inFlight.remove(packageName)
                }
            }
        }
    }

    private fun scrapePlayStoreIconUrl(packageName: String): String {
        return try {
            val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"
            val conn = (URL(storeUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                var matcher = ogImagePattern.matcher(html)
                var rawUrl = ""
                if (matcher.find()) {
                    rawUrl = matcher.group(1) ?: ""
                }
                if (rawUrl.isEmpty()) {
                    matcher = itemImagePattern.matcher(html)
                    if (matcher.find()) {
                        rawUrl = matcher.group(1) ?: ""
                    }
                }

                if (rawUrl.isNotEmpty()) {
                    // Google CDN Scaler: Replace existing size parameter or append =s512 for full 512x512 Ultra-HD
                    val baseUrl = if (rawUrl.contains("=")) rawUrl.substringBefore("=") else rawUrl
                    return "$baseUrl=s512"
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        var input: InputStream? = null
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
            }
            input = conn.inputStream
            BitmapFactory.decodeStream(input)
        } catch (_: Exception) {
            null
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }
    }
}
