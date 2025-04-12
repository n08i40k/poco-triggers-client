package ru.n08i40k.poco.triggers.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.MainActivity

class AccessibilityService : AccessibilityService() {
    private companion object {
        const val TAG = "AccessibilityService"

        val FORBIDDEN_APPS = listOf<String>(
            "excluded.package.names.here"
        )
        val FORBIDDEN_ACTIVITIES = listOf<String>(
            MainActivity::class.java.name
        )

        val SKIP_APPS = listOf<String>(
            "com.android.systemui"
        )
    }

    override fun onServiceConnected() {
        Log.d(TAG, "Accessibility service connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }

        this.serviceInfo = info
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            handleWindowStateChanged(event)
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val viewModel = (application as Application).viewModel

        val packageName = event.packageName.toString()
        val className = event.className.toString()

        if (className != MainActivity::class.java.name &&
            packageName == applicationContext.packageName
        ) return

        if (packageName in SKIP_APPS) {
            Log.d(TAG, "Skipping $packageName/$className!")
            return
        }

        if (className in FORBIDDEN_ACTIVITIES || packageName in FORBIDDEN_APPS) {
            Log.d(TAG, "Cancelling $packageName/$className!")
            viewModel.setPackageName(null)
            return
        }

        Log.d(TAG, "Writing package name $packageName into application view model...")
        viewModel.setPackageName(packageName)
    }
}