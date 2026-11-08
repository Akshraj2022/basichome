package com.minimal.tvlauncher.model

/**
 * Represents a hardware TV input (HDMI 1, HDMI 2, AV, etc.) for direct switching.
 */
data class TvInputItem(
    val id: String,
    val label: String,
    val isHdmi: Boolean = true,
    val hdmiPort: Int = 1
)
