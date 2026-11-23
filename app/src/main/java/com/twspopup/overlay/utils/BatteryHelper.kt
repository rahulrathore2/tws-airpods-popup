package com.twspopup.overlay.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log

object BatteryHelper {

    private const val TAG = "BatteryHelper"

    /**
     * Extracts real-time battery level of the Bluetooth device using reflection.
     * Android hidden API: BluetoothDevice.getBatteryLevel() returns battery level [0-100] or -1 if unavailable.
     */
    @SuppressLint("PrivateApi")
    fun getBatteryLevel(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            Log.d(TAG, "Reflection getBatteryLevel for ${device.address}: $level%")
            level
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "getBatteryLevel method not found on this Android build", e)
            -1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read battery level via reflection", e)
            -1
        }
    }

    /**
     * Attempts to read battery level from connection Intent extras if provided by the vendor.
     */
    fun getBatteryLevelFromIntent(intent: Intent): Int {
        // Some Android vendor builds broadcast battery level in intent extras
        val extraKeys = arrayOf(
            "android.bluetooth.device.extra.BATTERY_LEVEL",
            "EXTRA_BATTERY_LEVEL",
            "battery_level"
        )
        for (key in extraKeys) {
            if (intent.hasExtra(key)) {
                val level = intent.getIntExtra(key, -1)
                if (level in 0..100) return level
            }
        }
        return -1
    }
}
