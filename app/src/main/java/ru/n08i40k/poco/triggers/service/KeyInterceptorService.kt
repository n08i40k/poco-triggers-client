package ru.n08i40k.poco.triggers.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.utility.TouchEmulator

class KeyInterceptorService : AccessibilityService() {
    private companion object {
        const val TAG = "KeyInterceptor"
    }

    var touchEmulator: TouchEmulator? = null
    var lastEnable: Long = 0

    override fun onServiceConnected() {
        Log.d(TAG, "Service connected")

        touchEmulator = TouchEmulator()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED // или другие нужные типы
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS + AccessibilityServiceInfo.FLAG_SEND_MOTION_EVENTS
            notificationTimeout = 0
        }

        this.serviceInfo = info
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")

        touchEmulator?.shutdown()
        touchEmulator = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    private fun onTriggerEnable() {
        val now = System.currentTimeMillis()

        if ((now - lastEnable) < 500) {
            Log.d(TAG, "Starting intent...")

            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
        }

        lastEnable = now
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val result = super.onKeyEvent(event)

        if (event == null)
            return result

        when (event.keyCode) {
            KeyEvent.KEYCODE_F3 -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Upper trigger enabled")
                    onTriggerEnable()
                }
            }

            KeyEvent.KEYCODE_F4 -> {
                if (event.action == KeyEvent.ACTION_UP)
                    Log.d(TAG, "Upper trigger disabled")
            }

            KeyEvent.KEYCODE_F5 -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Lower trigger enabled")
                    onTriggerEnable()
                }
            }

            KeyEvent.KEYCODE_F6 -> {
                if (event.action == KeyEvent.ACTION_UP)
                    Log.d(TAG, "Lower trigger disabled")
            }

            KeyEvent.KEYCODE_F1 -> {
                if (!(applicationContext as Application).triggers.upper.enabled)
                    return result

                if (event.action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Upper trigger pressed")

                    val pos = (this.applicationContext as Application)
                        .triggers
                        .upper
                        .pos

                    touchEmulator?.beginTouch(1, pos.x, pos.y)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Upper trigger unpressed")

                    touchEmulator?.endTouch(1)
                }
            }

            KeyEvent.KEYCODE_F2 -> {
                if (!(applicationContext as Application).triggers.lower.enabled)
                    return result

                if (event.action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Lower trigger pressed")

                    val pos = (this.applicationContext as Application)
                        .triggers
                        .lower
                        .pos
                    touchEmulator?.beginTouch(0, pos.x, pos.y)

                } else if (event.action == KeyEvent.ACTION_UP) {
                    Log.d(TAG, "Lower trigger unpressed")

                    touchEmulator?.endTouch(0)
                }
            }
        }

        return result
    }
}