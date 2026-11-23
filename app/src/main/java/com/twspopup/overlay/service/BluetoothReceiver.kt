package com.twspopup.overlay.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.twspopup.overlay.data.PreferencesManager
import com.twspopup.overlay.overlay.OverlayPopupManager
import com.twspopup.overlay.utils.BatteryHelper
import com.twspopup.overlay.utils.PermissionHelper

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "BluetoothReceiver received action: $action")

        val prefs = PreferencesManager.getInstance(context)
        val targetMac = prefs.targetMacAddress

        if (targetMac.isNullOrEmpty()) {
            Log.d(TAG, "No target MAC address configured. Ignoring.")
            return
        }

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        if (device == null) {
            Log.w(TAG, "Received Bluetooth broadcast without EXTRA_DEVICE")
            return
        }

        val connectedAddress = device.address
        Log.d(TAG, "Device connected/disconnected: $connectedAddress vs target: $targetMac")

        if (!connectedAddress.equals(targetMac, ignoreCase = true)) {
            return
        }

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.i(TAG, "Target TWS connected! Triggering Apple AirPods-style pop-up...")

                // Resolve device name safely
                val deviceName = getDeviceName(context, device) ?: prefs.targetDeviceName ?: "Earbuds"

                // Extract battery level
                var battery = BatteryHelper.getBatteryLevel(device)
                if (battery !in 0..100) {
                    battery = BatteryHelper.getBatteryLevelFromIntent(intent)
                }

                val customImage = prefs.customImagePath

                // Show the system overlay pop-up
                OverlayPopupManager.show(
                    context = context,
                    deviceName = deviceName,
                    batteryLevel = battery,
                    customImagePath = customImage
                )
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.i(TAG, "Target TWS disconnected.")
                OverlayPopupManager.dismiss()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(context: Context, device: BluetoothDevice): String? {
        return if (PermissionHelper.hasBluetoothPermission(context)) {
            try {
                device.name
            } catch (e: SecurityException) {
                null
            }
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "BluetoothReceiver"
    }
}
