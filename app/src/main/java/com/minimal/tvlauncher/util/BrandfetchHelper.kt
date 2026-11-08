package com.minimal.tvlauncher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Brandfetch API integration.
 * Automatically searches, downloads and persistently stores the dark version of each app's official logo.
 * Executes strictly once per application and caches permanently in local device storage.
 */
object BrandfetchHelper {

    private const val API_KEY =
        "fATMpucmuddthVLAymJOEXHN_zZaTDHQItk46t3ky_ntbruLhAWajtP0oEWObpsZL90SOOceLK2TJx2biU1jww"

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val inFlight = HashSet<String>()

    // Instant direct domain mapping for popular TV packages to save search API calls
    private val directDomains = mapOf(
        "com.netflix.ninja" to "netflix.com",
        "com.google.android.youtube.tv" to "youtube.com",
        "com.amazon.amazonvideo.livingroom" to "primevideo.com",
        "in.startv.hotstar" to "hotstar.com",
        "com.crunchyroll.crunchyroid" to "crunchyroll.com",
        "tv.airtel.xstream.tvapp" to "airtel.in",
        "com.jio.media.jiotvplus" to "jio.com",
        "com.jio.media.stb.ondemand" to "jiocinema.com",
        "com.sonyliv" to "sonyliv.com",
        "com.google.android.videos" to "google.com",
        "com.google.android.katniss" to "google.com",
        "com.google.android.youtube.tvmusic" to "youtube.com",
        "com.mxtech.videoplayer.television" to "mxplayer.in",
        "com.ludo.king.tv" to "gametion.com",
        "com.vectorunit.purple.googleplay" to "vectorunit.com",
        "com.windscribe.vpn" to "windscribe.com",
        "ch.protonvpn.android" to "protonvpn.com",
        "com.sand.aircasttv" to "airdroid.com"
    )

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "brand_icons")
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

    fun loadStoredIcon(context: Context, packageName: String): Drawable? {
        val file = getIconFile(context, packageName)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            BitmapDrawable(context.resources, bitmap)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Asynchronously fetches the dark version of an app's logo from Brandfetch API
     * and permanently saves it to local disk storage.
     */
    fun fetchBrandLogoAsync(
        context: Context,
        packageName: String,
        appName: String,
        onSuccess: (Drawable) -> Unit
    ) {
        if (hasStoredIcon(context, packageName)) {
            loadStoredIcon(context, packageName)?.let { onSuccess(it) }
            return
        }

        synchronized(inFlight) {
            if (inFlight.contains(packageName)) return
            inFlight.add(packageName)
        }

        executor.execute {
            try {
                val domain = directDomains[packageName] ?: searchDomainForApp(appName)
                if (domain.isNotEmpty()) {
                    val logoUrl = fetchDarkLogoUrl(domain)
                    if (logoUrl.isNotEmpty()) {
                        val bitmap = downloadBitmap(logoUrl)
                        if (bitmap != null) {
                            // Strip any residual solid background if present
                            val processed = IconBackgroundRemover.processIcon(
                                context,
                                BitmapDrawable(context.resources, bitmap)
                            )
                            val finalBitmap = (processed as? BitmapDrawable)?.bitmap ?: bitmap

                            // Store in storage marked with its respective app package name
                            val destinationFile = getIconFile(context, packageName)
                            FileOutputStream(destinationFile).use { out ->
                                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }

                            val resultDrawable = BitmapDrawable(context.resources, finalBitmap)
                            mainHandler.post {
                                onSuccess(resultDrawable)
                            }
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

    private fun searchDomainForApp(appName: String): String {
        return try {
            val cleanName = appName.replace(Regex("(?i)Android TV|TV|for Android|App"), "").trim()
            val query = URLEncoder.encode(cleanName, "UTF-8")
            val url = URL("https://api.brandfetch.io/v2/search/$query")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $API_KEY")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(response)
                if (array.length() > 0) {
                    return array.getJSONObject(0).optString("domain", "")
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun fetchDarkLogoUrl(domain: String): String {
        return try {
            val url = URL("https://api.brandfetch.io/v2/brands/$domain")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $API_KEY")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val logos = json.optJSONArray("logos") ?: return ""

                var bestUrl = ""
                // 1st priority: symbol with theme "dark"
                // 2nd priority: icon with theme "dark"
                // 3rd priority: logo with theme "dark"
                // Fallback: any transparent PNG
                for (i in 0 until logos.length()) {
                    val item = logos.getJSONObject(i)
                    val type = item.optString("type")
                    val theme = item.optString("theme")
                    val formats = item.optJSONArray("formats") ?: continue

                    var candidatePng = ""
                    for (f in 0 until formats.length()) {
                        val formatObj = formats.getJSONObject(f)
                        val fmt = formatObj.optString("format")
                        val src = formatObj.optString("src")
                        if (fmt.equals("png", ignoreCase = true) || fmt.equals("webp", ignoreCase = true)) {
                            candidatePng = src
                            if (formatObj.optString("background").equals("transparent", ignoreCase = true)) {
                                break
                            }
                        }
                    }

                    if (candidatePng.isNotEmpty()) {
                        if (theme == "dark" && (type == "symbol" || type == "icon")) {
                            return candidatePng
                        }
                        if (bestUrl.isEmpty() || (theme == "dark" && type == "logo")) {
                            bestUrl = candidatePng
                        }
                    }
                }
                return bestUrl
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        var input: InputStream? = null
        return try {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
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
