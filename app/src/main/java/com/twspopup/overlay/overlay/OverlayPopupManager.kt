package com.twspopup.overlay.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.twspopup.overlay.R
import com.twspopup.overlay.utils.PermissionHelper
import java.io.File

object OverlayPopupManager {

    private const val TAG = "OverlayPopupManager"
    private const val AUTO_DISMISS_DELAY_MS = 5000L

    private var windowManager: WindowManager? = null
    private var popupView: View? = null
    private var isShowing = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable {
        dismiss()
    }

    /**
     * Display the AirPods-style animated bottom-sheet pop-up overlay.
     */
    fun show(
        context: Context,
        deviceName: String,
        batteryLevel: Int,
        customImagePath: String?
    ) {
        mainHandler.post {
            if (!PermissionHelper.hasOverlayPermission(context)) {
                Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission is not granted")
                return@post
            }

            // If already showing, cancel dismiss timer and update UI or re-animate
            if (isShowing && popupView != null) {
                mainHandler.removeCallbacks(autoDismissRunnable)
                updateUI(deviceName, batteryLevel, customImagePath)
                mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY_MS)
                return@post
            }

            try {
                val appContext = context.applicationContext
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager = wm

                val inflater = LayoutInflater.from(appContext)
                val view = inflater.inflate(R.layout.layout_airpods_popup, null)
                popupView = view

                // WindowManager LayoutParams for System Overlay
                val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    windowAnimations = 0 // Using custom programmatic animation for perfect spring effect
                }

                // Bind UI elements
                val cardPopup = view.findViewById<CardView>(R.id.cardPopup)
                val txtDeviceName = view.findViewById<TextView>(R.id.txtDeviceName)
                val txtBatteryLevel = view.findViewById<TextView>(R.id.txtBatteryLevel)
                val badgeBattery = view.findViewById<LinearLayout>(R.id.badgeBattery)
                val imgEarbuds = view.findViewById<ImageView>(R.id.imgEarbuds)
                val btnDone = view.findViewById<Button>(R.id.btnDone)

                txtDeviceName.text = deviceName.ifBlank { "Wireless Earbuds" }

                // Set Battery Level
                if (batteryLevel in 0..100) {
                    badgeBattery.visibility = View.VISIBLE
                    txtBatteryLevel.text = "$batteryLevel%"
                } else {
                    badgeBattery.visibility = View.GONE
                }

                // Load custom image if available and valid
                loadEarbudsImage(imgEarbuds, customImagePath)

                // Done button dismisses overlay immediately
                btnDone.setOnClickListener {
                    dismiss()
                }

                // Attach to Window
                wm.addView(view, params)
                isShowing = true

                // Start slide-up animation
                startEntranceAnimation(cardPopup)

                // Schedule auto-dismiss after 5 seconds
                mainHandler.removeCallbacks(autoDismissRunnable)
                mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error displaying overlay popup", e)
                isShowing = false
                popupView = null
            }
        }
    }

    private fun updateUI(deviceName: String, batteryLevel: Int, customImagePath: String?) {
        val view = popupView ?: return
        val txtDeviceName = view.findViewById<TextView>(R.id.txtDeviceName)
        val txtBatteryLevel = view.findViewById<TextView>(R.id.txtBatteryLevel)
        val badgeBattery = view.findViewById<LinearLayout>(R.id.badgeBattery)
        val imgEarbuds = view.findViewById<ImageView>(R.id.imgEarbuds)

        txtDeviceName.text = deviceName.ifBlank { "Wireless Earbuds" }
        if (batteryLevel in 0..100) {
            badgeBattery.visibility = View.VISIBLE
            txtBatteryLevel.text = "$batteryLevel%"
        } else {
            badgeBattery.visibility = View.GONE
        }
        loadEarbudsImage(imgEarbuds, customImagePath)
    }

    private fun loadEarbudsImage(imageView: ImageView, path: String?) {
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom image, falling back to default", e)
                }
            }
        }
        imageView.setImageResource(R.drawable.ic_earbuds_default)
    }

    private fun startEntranceAnimation(cardView: View) {
        // Initial state: translated down 600px and faded
        cardView.translationY = 600f
        cardView.alpha = 0f

        cardView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(450)
            .setInterpolator(OvershootInterpolator(0.85f))
            .start()
    }

    fun dismiss() {
        mainHandler.post {
            if (!isShowing || popupView == null) return@post

            mainHandler.removeCallbacks(autoDismissRunnable)

            val currentView = popupView ?: return@post
            val cardView = currentView.findViewById<CardView>(R.id.cardPopup)

            cardView?.animate()
                ?.translationY(700f)
                ?.alpha(0f)
                ?.setDuration(320)
                ?.setInterpolator(DecelerateInterpolator())
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        removePopupFromWindow()
                    }
                })?.start() ?: removePopupFromWindow()
        }
    }

    private fun removePopupFromWindow() {
        try {
            if (popupView != null && windowManager != null) {
                windowManager?.removeViewImmediate(popupView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay popup view", e)
        } finally {
            popupView = null
            isShowing = false
        }
    }
}
