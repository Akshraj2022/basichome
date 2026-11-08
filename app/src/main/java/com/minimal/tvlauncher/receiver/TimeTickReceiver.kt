package com.minimal.tvlauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver updating the 12-hour "h:mm" clock on ACTION_TIME_TICK.
 */
class TimeTickReceiver(private val onTimeUpdate: (String) -> Unit) : BroadcastReceiver() {

    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    override fun onReceive(context: Context?, intent: Intent?) {
        onTimeUpdate(getCurrentTime())
    }

    fun getCurrentTime(): String {
        return timeFormat.format(Date())
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(this, filter)
        onTimeUpdate(getCurrentTime())
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: Exception) {}
    }
}
