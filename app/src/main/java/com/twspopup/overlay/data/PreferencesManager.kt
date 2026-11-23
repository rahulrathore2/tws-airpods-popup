package com.twspopup.overlay.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var targetMacAddress: String?
        get() = prefs.getString(KEY_TARGET_MAC, null)
        set(value) = prefs.edit().putString(KEY_TARGET_MAC, value).apply()

    var targetDeviceName: String?
        get() = prefs.getString(KEY_TARGET_NAME, null)
        set(value) = prefs.edit().putString(KEY_TARGET_NAME, value).apply()

    var customImagePath: String?
        get() = prefs.getString(KEY_CUSTOM_IMAGE, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_IMAGE, value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    fun clearTargetDevice() {
        prefs.edit()
            .remove(KEY_TARGET_MAC)
            .remove(KEY_TARGET_NAME)
            .apply()
    }

    fun clearCustomImage() {
        prefs.edit().remove(KEY_CUSTOM_IMAGE).apply()
    }

    companion object {
        private const val PREFS_NAME = "tws_airpods_prefs"
        private const val KEY_TARGET_MAC = "target_mac_address"
        private const val KEY_TARGET_NAME = "target_device_name"
        private const val KEY_CUSTOM_IMAGE = "custom_image_path"
        private const val KEY_SERVICE_ENABLED = "service_enabled"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
