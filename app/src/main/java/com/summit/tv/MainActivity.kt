package com.summit.tv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var summitView: SummitHomeView

    private val handler = Handler(Looper.getMainLooper())

    private var isScreensaver = false

    private val inactivityTime = 120 * 1000L

    private val screensaverRunnable = Runnable {
        showScreensaver()
    }

    @Suppress("DEPRECATION")
    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enableImmersiveMode()

        summitView = SummitHomeView(this)

        setContentView(summitView)

        resetInactivityTimer()
    }

    private fun showScreensaver() {
        isScreensaver = true

        summitView.showScreensaver()
    }

    private fun showHome() {
        isScreensaver = false

        summitView.showHome()

        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        handler.removeCallbacks(screensaverRunnable)

        if (!isScreensaver) {
            handler.postDelayed(
                screensaverRunnable,
                inactivityTime
            )
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (isScreensaver) {
            showHome()
        } else {
            resetInactivityTimer()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isScreensaver) {
            resetInactivityTimer()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(screensaverRunnable)
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}