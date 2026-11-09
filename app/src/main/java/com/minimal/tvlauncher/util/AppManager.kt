package com.minimal.tvlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.minimal.tvlauncher.model.AppItem
import java.util.Locale

/**
 * Discovers and manages launchable Leanback and standard Android applications.
 * Automatically classifies installed apps into dedicated categories:
 * Entertainment, Media & Music, Games, Tools & VPN, and System.
 */
object AppManager {

    private val EXCLUDED_PACKAGES = setOf(
        "com.minimal.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tv.launcher"
    )

    fun getAllLaunchableApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val appsMap = mutableMapOf<String, AppItem>()

        // 1. Query Android TV Leanback Launcher intent
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val leanbackActivities = pm.queryIntentActivities(leanbackIntent, PackageManager.MATCH_ALL)
        for (ri in leanbackActivities) {
            val pkg = ri.activityInfo.packageName
            if (!EXCLUDED_PACKAGES.contains(pkg)) {
                val label = ri.loadLabel(pm).toString().trim()
                appsMap[pkg] = AppItem(packageName = pkg, label = label)
            }
        }

        // 2. Query Standard Launcher intent
        val standardIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val standardActivities = pm.queryIntentActivities(standardIntent, PackageManager.MATCH_ALL)
        for (ri in standardActivities) {
            val pkg = ri.activityInfo.packageName
            if (!EXCLUDED_PACKAGES.contains(pkg) && !appsMap.containsKey(pkg)) {
                val label = ri.loadLabel(pm).toString().trim()
                appsMap[pkg] = AppItem(packageName = pkg, label = label)
            }
        }

        return appsMap.values.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun getAppItem(context: Context, packageName: String): AppItem? {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString().trim()
            AppItem(packageName = packageName, label = label)
        } catch (_: Exception) {
            null
        }
    }

    fun isGame(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return true
            }
            @Suppress("DEPRECATION")
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return true

            val lowerPkg = packageName.lowercase(Locale.getDefault())
            lowerPkg.contains("game") || lowerPkg.contains("ludo") || lowerPkg.contains("racing") ||
                    lowerPkg.contains("unity") || lowerPkg.contains("party") || lowerPkg.contains("babel") ||
                    lowerPkg.contains("console")
        } catch (_: Exception) {
            false
        }
    }

    fun initDefaultRowsIfFirstRun(context: Context) {
        val prefManager = PreferenceManager(context)
        ensureAllCategoriesPopulated(context, prefManager)
    }

    fun ensureAllCategoriesPopulated(context: Context, prefManager: PreferenceManager) {
        val allApps = getAllLaunchableApps(context)
        val allPkgs = allApps.map { it.packageName }.toSet()

        // 1. Entertainment Priority
        if (prefManager.getEntertainmentRow().isEmpty()) {
            val entertainmentPriority = listOf(
                "com.netflix.ninja",
                "com.google.android.youtube.tv",
                "com.amazon.amazonvideo.livingroom",
                "com.disney.disneyplus",
                "com.disney.disneyplus.tv",
                "in.startv.hotstar",
                "in.startv.hotstar.dplus",
                "com.disney.disneyplus.androidtv",
                "com.jio.media.stb.ondemand",
                "tv.airtel.xstream.tvapp",
                "com.sonyliv",
                "com.graymatrix.did",
                "com.crunchyroll.crunchyroid",
                "com.neotv.app"
            )
            val pinned = pickMatching(entertainmentPriority, allPkgs, emptySet())
            if (pinned.size < 4) {
                for (app in allApps) {
                    if (!pinned.contains(app.packageName)) {
                        pinned.add(app.packageName)
                        if (pinned.size == 4) break
                    }
                }
            }
            prefManager.saveEntertainmentRow(pinned)
        }

        // 2. Media & Music Priority
        if (prefManager.getMediaRow().isEmpty()) {
            val mediaPriority = listOf(
                "com.google.android.youtube.tvmusic",
                "com.mxtech.videoplayer.television",
                "com.apps.usbmediaplayer",
                "com.mitv.tvhome.mitvplus",
                "com.google.android.videos"
            )
            val used = prefManager.getEntertainmentRow().toSet()
            val pinned = pickMatching(mediaPriority, allPkgs, used)
            if (pinned.size < 4) {
                for (app in allApps) {
                    if (!used.contains(app.packageName) && !pinned.contains(app.packageName)) {
                        pinned.add(app.packageName)
                        if (pinned.size == 4) break
                    }
                }
            }
            prefManager.saveMediaRow(pinned)
        }

        // 3. Games Priority
        if (prefManager.getGamesRow().isEmpty()) {
            val gamePriority = listOf(
                "com.vectorunit.purple.googleplay",
                "com.ludo.king.tv",
                "com.xformgames.burninrubber5air",
                "ch.dnastudios.towerofbabel",
                "com.airconsole.androidtv",
                "com.beetsteam.ludogame",
                "ch.dnastudios.theneighbourhood",
                "com.epopeiagames.monsterparty"
            )
            val pinned = pickMatching(gamePriority, allPkgs, emptySet())
            if (pinned.size < 4) {
                for (app in allApps) {
                    if (isGame(context, app.packageName) && !pinned.contains(app.packageName)) {
                        pinned.add(app.packageName)
                        if (pinned.size == 4) break
                    }
                }
            }
            prefManager.saveGamesRow(pinned)
        }

        // 4. Tools & VPN Priority
        if (prefManager.getToolsRow().isEmpty()) {
            val toolsPriority = listOf(
                "ch.protonvpn.android",
                "com.windscribe.vpn",
                "com.free.vpn.planet.tv",
                "canada.vpn",
                "com.sand.aircasttv",
                "com.creative.fastscreen.tv"
            )
            val used = (prefManager.getEntertainmentRow() + prefManager.getMediaRow() + prefManager.getGamesRow()).toSet()
            val pinned = pickMatching(toolsPriority, allPkgs, used)
            if (pinned.size < 4) {
                for (app in allApps) {
                    val lp = app.packageName.lowercase(Locale.getDefault())
                    if ((lp.contains("vpn") || lp.contains("cast") || lp.contains("screen") || lp.contains("air")) &&
                        !used.contains(app.packageName) && !pinned.contains(app.packageName)
                    ) {
                        pinned.add(app.packageName)
                        if (pinned.size == 4) break
                    }
                }
            }
            prefManager.saveToolsRow(pinned)
        }

        // 5. System & Utility Priority
        if (prefManager.getUtilityRow().isEmpty()) {
            val systemPriority = listOf(
                "com.android.tv.settings",
                "com.android.settings",
                "com.android.vending",
                "com.apps.tvmanager",
                "com.google.android.katniss",
                "com.apps.ota",
                "com.realsil.ota"
            )
            val used = (prefManager.getEntertainmentRow() + prefManager.getMediaRow() +
                    prefManager.getGamesRow() + prefManager.getToolsRow()).toSet()
            val pinned = pickMatching(systemPriority, allPkgs, used)
            if (pinned.size < 4) {
                for (app in allApps) {
                    if (!used.contains(app.packageName) && !pinned.contains(app.packageName)) {
                        pinned.add(app.packageName)
                        if (pinned.size == 4) break
                    }
                }
            }
            prefManager.saveUtilityRow(pinned)
        }

        // 6. Recent Apps
        if (prefManager.getRecentApps().isEmpty()) {
            val recents = prefManager.getEntertainmentRow().take(4).ifEmpty {
                allApps.take(4).map { it.packageName }
            }
            for (pkg in recents) {
                prefManager.addRecentApp(pkg)
            }
        }

        prefManager.isFirstRun = false
    }

    private fun pickMatching(priorityList: List<String>, available: Set<String>, exclude: Set<String>): MutableList<String> {
        val result = mutableListOf<String>()
        for (pkg in priorityList) {
            if (available.contains(pkg) && !exclude.contains(pkg) && !result.contains(pkg)) {
                result.add(pkg)
                if (result.size == 4) break
            }
        }
        return result
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            return try {
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }
}
