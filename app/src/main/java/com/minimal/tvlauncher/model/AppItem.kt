package com.minimal.tvlauncher.model

/**
 * Represents an application item displayed in launcher rows or grid.
 */
data class AppItem(
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val isMoreTile: Boolean = false,
    val isLeanback: Boolean = true
)
