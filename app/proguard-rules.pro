# Add project specific ProGuard rules here.
# Keep Bluetooth reflection method
-keepclassmembers class android.bluetooth.BluetoothDevice {
    public int getBatteryLevel();
}
