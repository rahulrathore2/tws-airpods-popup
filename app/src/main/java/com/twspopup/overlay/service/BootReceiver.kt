package com.twspopup.overlay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.twspopup.overlay.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("BootReceiver", "Device booted or app replaced. Checking service state...")
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.isServiceEnabled && !prefs.targetMacAddress.isNullOrEmpty()) {
                Log.i("BootReceiver", "Auto-starting BluetoothMonitorService...")
                BluetoothMonitorService.start(context)
            }
        }
    }
}
