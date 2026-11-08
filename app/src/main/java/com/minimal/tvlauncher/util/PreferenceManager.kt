package com.minimal.tvlauncher.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Manages persisted pinned packages for all categories:
 * Recent, Entertainment, Media & Music, Games, Tools & VPN, and System rows.
 */
class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tv_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_RUN = "is_first_run"
        private const val KEY_RECENT_APPS = "recent_apps"
        private const val KEY_ENTERTAINMENT_ROW = "pinned_entertainment_row"
        private const val KEY_MEDIA_ROW = "pinned_media_row"
        private const val KEY_GAMES_ROW = "pinned_games_row"
        private const val KEY_TOOLS_ROW = "pinned_tools_row"
        private const val KEY_UTILITY_ROW = "pinned_utility_row"
    }

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    fun getRecentApps(): MutableList<String> {
        val jsonString = prefs.getString(KEY_RECENT_APPS, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun addRecentApp(packageName: String) {
        val list = getRecentApps()
        list.remove(packageName)
        list.add(0, packageName)
        while (list.size > 4) {
            list.removeAt(list.size - 1)
        }
        prefs.edit().putString(KEY_RECENT_APPS, toJsonList(list)).apply()
    }

    fun getEntertainmentRow(): MutableList<String> {
        val jsonString = prefs.getString(KEY_ENTERTAINMENT_ROW, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun saveEntertainmentRow(packages: List<String>) {
        prefs.edit().putString(KEY_ENTERTAINMENT_ROW, toJsonList(packages)).apply()
    }

    fun getMediaRow(): MutableList<String> {
        val jsonString = prefs.getString(KEY_MEDIA_ROW, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun saveMediaRow(packages: List<String>) {
        prefs.edit().putString(KEY_MEDIA_ROW, toJsonList(packages)).apply()
    }

    fun getGamesRow(): MutableList<String> {
        val jsonString = prefs.getString(KEY_GAMES_ROW, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun saveGamesRow(packages: List<String>) {
        prefs.edit().putString(KEY_GAMES_ROW, toJsonList(packages)).apply()
    }

    fun getToolsRow(): MutableList<String> {
        val jsonString = prefs.getString(KEY_TOOLS_ROW, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun saveToolsRow(packages: List<String>) {
        prefs.edit().putString(KEY_TOOLS_ROW, toJsonList(packages)).apply()
    }

    fun getUtilityRow(): MutableList<String> {
        val jsonString = prefs.getString(KEY_UTILITY_ROW, null) ?: return mutableListOf()
        return parseJsonList(jsonString)
    }

    fun saveUtilityRow(packages: List<String>) {
        prefs.edit().putString(KEY_UTILITY_ROW, toJsonList(packages)).apply()
    }

    private fun parseJsonList(json: String): MutableList<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val pkg = array.optString(i)
                if (!pkg.isNullOrEmpty()) {
                    result.add(pkg)
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun toJsonList(list: List<String>): String {
        val array = JSONArray()
        for (item in list) {
            array.put(item)
        }
        return array.toString()
    }
}
