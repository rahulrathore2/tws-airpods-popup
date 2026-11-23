package com.twspopup.overlay

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.twspopup.overlay.data.DeviceItem
import com.twspopup.overlay.data.PreferencesManager
import com.twspopup.overlay.databinding.ActivityMainBinding
import com.twspopup.overlay.overlay.OverlayPopupManager
import com.twspopup.overlay.service.BluetoothMonitorService
import com.twspopup.overlay.ui.DeviceAdapter
import com.twspopup.overlay.utils.PermissionHelper
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var deviceAdapter: DeviceAdapter

    // Bluetooth permission launcher (Android 12+)
    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updatePermissionStatus()
            loadPairedDevices()
        } else {
            Toast.makeText(this, "Bluetooth Connect permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification permission launcher (Android 13+)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatePermissionStatus()
        if (!isGranted) {
            Toast.makeText(this, "Notification permission helps keep service alive", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery PNG Image Picker
    private val pickEarbudsImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveImageToInternalStorage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        loadPairedDevices()
        updateServiceState()
    }

    private fun initViews() {
        // Setup paired devices RecyclerView
        deviceAdapter = DeviceAdapter(emptyList()) { selectedItem ->
            prefs.targetMacAddress = selectedItem.address
            prefs.targetDeviceName = selectedItem.name
            updateSelectedDeviceText()
            Toast.makeText(this, "Target set: ${selectedItem.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = deviceAdapter

        updateSelectedDeviceText()
        loadSavedEarbudsImage()

        // Hide notification permission row on Android 12 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.rowPermNotification.visibility = View.GONE
            binding.dividerNotification.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        // Permission Buttons
        binding.btnPermOverlay.setOnClickListener {
            val intent = PermissionHelper.getOverlayPermissionIntent(this)
            startActivity(intent)
        }

        binding.btnPermBluetooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBluetoothPermission.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                Toast.makeText(this, "Bluetooth permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPermNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Refresh devices list
        binding.btnRefreshDevices.setOnClickListener {
            loadPairedDevices()
        }

        // Custom PNG Image Picker
        binding.btnPickImage.setOnClickListener {
            pickEarbudsImage.launch("image/*")
        }

        // Reset Custom Image
        binding.btnResetImage.setOnClickListener {
            val oldPath = prefs.customImagePath
            if (!oldPath.isNullOrEmpty()) {
                File(oldPath).delete()
            }
            prefs.clearCustomImage()
            binding.imgPreviewEarbuds.setImageResource(R.drawable.ic_earbuds_default)
            Toast.makeText(this, "Reset to default image", Toast.LENGTH_SHORT).show()
        }

        // Background Service Switch
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (prefs.targetMacAddress.isNullOrEmpty()) {
                    Toast.makeText(this, "Please select a paired earbuds device first!", Toast.LENGTH_LONG).show()
                    binding.switchService.isChecked = false
                    return@setOnCheckedChangeListener
                }
                if (!PermissionHelper.hasOverlayPermission(this)) {
                    Toast.makeText(this, "Overlay permission is required to display pop-up", Toast.LENGTH_LONG).show()
                    binding.switchService.isChecked = false
                    return@setOnCheckedChangeListener
                }
                prefs.isServiceEnabled = true
                BluetoothMonitorService.start(this)
            } else {
                prefs.isServiceEnabled = false
                BluetoothMonitorService.stop(this)
            }
            updateServiceState()
        }

        // Test Pop-up Overlay preview
        binding.btnTestPopup.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                Toast.makeText(this, "Please grant 'Display Over Other Apps' permission first", Toast.LENGTH_LONG).show()
                val intent = PermissionHelper.getOverlayPermissionIntent(this)
                startActivity(intent)
                return@setOnClickListener
            }

            val testName = prefs.targetDeviceName ?: "AirPods Pro (Test)"
            val customImg = prefs.customImagePath

            OverlayPopupManager.show(
                context = this,
                deviceName = testName,
                batteryLevel = 90,
                customImagePath = customImg
            )
        }
    }

    private fun updatePermissionStatus() {
        // Overlay Permission
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        binding.btnPermOverlay.isEnabled = !hasOverlay
        binding.btnPermOverlay.text = if (hasOverlay) getString(R.string.permission_granted) else getString(R.string.grant_permission)

        // Bluetooth Permission
        val hasBluetooth = PermissionHelper.hasBluetoothPermission(this)
        binding.btnPermBluetooth.isEnabled = !hasBluetooth
        binding.btnPermBluetooth.text = if (hasBluetooth) getString(R.string.permission_granted) else getString(R.string.grant_permission)

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotification = PermissionHelper.hasNotificationPermission(this)
            binding.btnPermNotification.isEnabled = !hasNotification
            binding.btnPermNotification.text = if (hasNotification) getString(R.string.permission_granted) else getString(R.string.grant_permission)
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        if (!PermissionHelper.hasBluetoothPermission(this)) {
            binding.txtNoDevices.visibility = View.VISIBLE
            binding.txtNoDevices.text = "Bluetooth permission required to list paired devices."
            return
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            binding.txtNoDevices.visibility = View.VISIBLE
            binding.txtNoDevices.text = "Bluetooth is turned off or not available on this device."
            deviceAdapter.updateData(emptyList(), prefs.targetMacAddress)
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = adapter.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            binding.txtNoDevices.visibility = View.VISIBLE
            binding.txtNoDevices.text = getString(R.string.no_paired_devices)
            deviceAdapter.updateData(emptyList(), prefs.targetMacAddress)
        } else {
            binding.txtNoDevices.visibility = View.GONE
            val list = pairedDevices.map { device ->
                DeviceItem(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    isSelected = device.address.equals(prefs.targetMacAddress, ignoreCase = true)
                )
            }
            deviceAdapter.updateData(list, prefs.targetMacAddress)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val destinationFile = File(filesDir, "custom_earbuds.png")
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                prefs.customImagePath = destinationFile.absolutePath
                loadSavedEarbudsImage()
                Toast.makeText(this, "Custom earbuds image saved!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedEarbudsImage() {
        val path = prefs.customImagePath
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.imgPreviewEarbuds.setImageBitmap(bitmap)
                    return
                }
            }
        }
        binding.imgPreviewEarbuds.setImageResource(R.drawable.ic_earbuds_default)
    }

    private fun updateSelectedDeviceText() {
        val mac = prefs.targetMacAddress
        val name = prefs.targetDeviceName
        if (mac.isNullOrEmpty()) {
            binding.txtSelectedMacStatus.text = getString(R.string.no_device_selected)
        } else {
            binding.txtSelectedMacStatus.text = "Selected: ${name ?: "Earbuds"} ($mac)"
        }
    }

    private fun updateServiceState() {
        val isRunning = BluetoothMonitorService.isRunning
        binding.switchService.isChecked = isRunning
        binding.txtServiceStatus.text = if (isRunning) {
            getString(R.string.service_running)
        } else {
            getString(R.string.service_stopped)
        }
    }
}
