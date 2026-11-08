package com.minimal.tvlauncher.util

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import com.minimal.tvlauncher.model.TvInputItem

/**
 * Discovers and launches hardware TV inputs (HDMI 1, HDMI 2, HDMI 3, AV) via TvInputManager.
 */
object TvInputHelper {

    fun getTvInputs(context: Context): List<TvInputItem> {
        val results = mutableListOf<TvInputItem>()
        try {
            val tvInputManager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            val inputList = tvInputManager?.tvInputList

            if (!inputList.isNullOrEmpty()) {
                var hdmiCounter = 1
                for (info in inputList) {
                    if (info.isPassthroughInput) {
                        val label = info.loadLabel(context)?.toString() ?: ""
                        val isHdmi = info.type == TvInputInfo.TYPE_HDMI
                        val displayLabel = when {
                            label.isNotEmpty() -> label
                            isHdmi -> "HDMI $hdmiCounter"
                            info.type == TvInputInfo.TYPE_COMPOSITE -> "AV"
                            info.type == TvInputInfo.TYPE_COMPONENT -> "Component"
                            else -> "Input $hdmiCounter"
                        }
                        results.add(
                            TvInputItem(
                                id = info.id,
                                label = displayLabel,
                                isHdmi = isHdmi,
                                hdmiPort = hdmiCounter
                            )
                        )
                        if (isHdmi) hdmiCounter++
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback default inputs if system list is empty or restricted
        if (results.isEmpty()) {
            results.add(TvInputItem(id = "HDMI1", label = "HDMI 1", isHdmi = true, hdmiPort = 1))
            results.add(TvInputItem(id = "HDMI2", label = "HDMI 2", isHdmi = true, hdmiPort = 2))
            results.add(TvInputItem(id = "HDMI3", label = "HDMI 3", isHdmi = true, hdmiPort = 3))
            results.add(TvInputItem(id = "AV", label = "AV", isHdmi = false, hdmiPort = 0))
        }

        return results
    }

    /**
     * Switches to the selected hardware TV input using Android TV passthrough URI.
     */
    fun launchInput(context: Context, input: TvInputItem): Boolean {
        return try {
            val uri: Uri = if (input.id.contains("/")) {
                TvContract.buildChannelUriForPassthroughInput(input.id)
            } else {
                TvContract.buildChannelUriForPassthroughInput(
                    "com.realtek.tv.passthrough/.hdmiinput.HDMITvInputService/HW151519488"
                )
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback: try opening Live TV or Input picker intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
