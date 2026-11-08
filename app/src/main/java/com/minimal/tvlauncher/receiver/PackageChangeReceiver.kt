package com.minimal.tvlauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * BroadcastReceiver monitoring package installs, updates and removals to refresh launcher rows.
 */
class PackageChangeReceiver(private val onPackageChanged: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        onPackageChanged()
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: Exception) {}
    }
}
