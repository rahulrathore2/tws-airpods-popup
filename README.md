# TWS AirPods-Style Animated Bottom-Sheet Pop-up Overlay (Android - Kotlin)

A lightweight, modern Android application written in Kotlin that brings Apple AirPods-style animated bottom-sheet pop-ups to any Bluetooth TWS (True Wireless Stereo) earbuds on Android.

---

## ✨ Features

1. **Setup Screen (`MainActivity.kt`)**:
   - Lists all paired Bluetooth devices with one-tap selection to save your earbuds' MAC address.
   - Built-in gallery image picker allowing you to select a custom PNG image of your earbuds. The image is saved to app-internal storage so it persists indefinitely across restarts.
   - Live permissions management cards with status indicators.
   - Foreground Service toggle switch.
   - **"Preview Pop-up Now"** button to instantly test the slide-up card animation without having to disconnect/reconnect your earbuds.

2. **System Overlay Pop-up (`OverlayPopupManager.kt`)**:
   - Displays a floating Apple-inspired bottom card over all apps and the home screen using `WindowManager` and `TYPE_APPLICATION_OVERLAY`.
   - Hardware-accelerated entrance spring animation (overshoot interpolator) and smooth dismissal slide-down animation.
   - Displays your custom earbuds PNG (or built-in vector icon), device name, connected badge with green indicator dot, and real-time battery level.
   - Includes a sleek **Done** button and an automatic **5-second auto-dismiss** timer.

3. **Real-Time Battery Extraction (`BatteryHelper.kt`)**:
   - Accesses real-time Bluetooth battery percentage via reflection on `BluetoothDevice.getBatteryLevel()`.
   - Fallback extraction from broadcast intent extras (`android.bluetooth.device.extra.BATTERY_LEVEL`).

4. **Background Listener (`BluetoothMonitorService.kt` & `BluetoothReceiver.kt`)**:
   - Monitors `BluetoothDevice.ACTION_ACL_CONNECTED` and `BluetoothDevice.ACTION_ACL_DISCONNECTED`.
   - Runs as an Android 14+ compliant Foreground Service (`foregroundServiceType="connectedDevice"`).
   - Starts automatically on boot (`BootReceiver.kt`) if enabled.

5. **Permission Management (`PermissionHelper.kt`)**:
   - Handles `SYSTEM_ALERT_WINDOW` (Display Over Other Apps).
   - Handles Android 12+ `BLUETOOTH_CONNECT`.
   - Handles Android 13+ `POST_NOTIFICATIONS`.

---

## 📁 Project Structure

```
tws-popup/
├── build.gradle.kts                      # Root Gradle configuration
├── settings.gradle.kts                   # Project settings
├── gradle.properties                     # JVM & AndroidX properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties         # Gradle 8.5 wrapper configuration
└── app/
    ├── build.gradle.kts                  # App module build config (SDK 34, ViewBinding)
    ├── proguard-rules.pro                # Reflection keep rules for getBatteryLevel()
    └── src/main/
        ├── AndroidManifest.xml           # Permissions, services, receivers, activities
        ├── java/com/twspopup/overlay/
        │   ├── MainActivity.kt           # Setup UI, image picker, device list & test trigger
        │   ├── data/
        │   │   ├── DeviceItem.kt         # Paired device model
        │   │   └── PreferencesManager.kt # SharedPreferences storage for MAC & image
        │   ├── overlay/
        │   │   └── OverlayPopupManager.kt# WindowManager pop-up controller & animations
        │   ├── service/
        │   │   ├── BluetoothMonitorService.kt # ConnectedDevice Foreground Service
        │   │   ├── BluetoothReceiver.kt  # ACL_CONNECTED / ACL_DISCONNECTED listener
        │   │   └── BootReceiver.kt       # Auto-start service on phone restart
        │   ├── ui/
        │   │   └── DeviceAdapter.kt      # RecyclerView adapter for paired Bluetooth devices
        │   └── utils/
        │       ├── BatteryHelper.kt      # Reflection helper for getBatteryLevel()
        │       └── PermissionHelper.kt   # Runtime & overlay permission utilities
        └── res/
            ├── anim/
            │   ├── popup_slide_up.xml    # Decelerated entrance slide animation
            │   └── popup_slide_down.xml  # Smooth dismissal slide animation
            ├── drawable/
            │   ├── bg_airpods_card.xml   # Rounded 32dp card background
            │   ├── bg_badge_connected.xml# Pill badge for status & battery
            │   ├── bg_button_done.xml    # Apple-style pill button with ripple
            │   ├── bg_device_item.xml    # List item card background
            │   ├── ic_earbuds_default.xml# Vector TWS earbuds graphic
            │   ├── ic_battery.xml        # Battery indicator icon
            │   ├── ic_bluetooth.xml      # Bluetooth icon
            │   ├── ic_check.xml          # Selection checkmark
            │   ├── ic_dot_green.xml      # Active green status dot
            │   ├── ic_image.xml          # Gallery image icon
            │   └── ic_refresh.xml        # Refresh icon
            ├── layout/
            │   ├── activity_main.xml     # Setup screen layout
            │   ├── item_bluetooth_device.xml # Device item card layout
            │   └── layout_airpods_popup.xml  # AirPods-style overlay popup layout
            └── values/
                ├── colors.xml            # Apple iOS inspired color palette
                ├── strings.xml           # Localized strings
                └── themes.xml            # App and window overlay styles
```

---

## 🛠️ How to Build & Run

### In Android Studio:
1. Open Android Studio and select **Open** -> Select `/data/user/0/ai.antigem/files/home/projects/tws-popup` (or clone the repository).
2. Allow Gradle sync to complete (uses Gradle 8.5, Android Gradle Plugin 8.2.2, compileSdk 34).
3. Connect your Android phone or start an emulator running Android 8.0+ (API 26+) or Android 12–14.
4. Click **Run 'app'** (`Shift + F10`).

### Via Command Line:
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🚀 How to Use

1. **Launch the App**: Open **TWS AirPods Pop-up**.
2. **Grant Permissions**:
   - Tap **Grant** under **Display Over Other Apps** and toggle permission ON.
   - Tap **Grant** under **Bluetooth Access** (Android 12+).
   - Tap **Grant** under **Notifications** (Android 13+).
3. **Select Your Device**:
   - Tap your paired TWS earbuds from the paired devices list.
4. **Choose Custom Earbuds Image**:
   - Tap **Choose Custom PNG** and select any transparent PNG image of your earbuds from your gallery.
5. **Start Service**:
   - Turn on the **Background Service** switch.
6. **Test**:
   - Tap **Preview Pop-up Now** to watch the AirPods-style animated card slide up!
   - Now disconnect and reconnect your earbuds or turn them on; the pop-up will appear automatically.
