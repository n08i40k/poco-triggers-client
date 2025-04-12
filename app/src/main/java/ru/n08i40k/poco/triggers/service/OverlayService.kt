package ru.n08i40k.poco.triggers.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.CombinedVibration
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ru.n08i40k.poco.triggers.R
import ru.n08i40k.poco.triggers.ui.model.OverlayViewModel
import ru.n08i40k.poco.triggers.ui.overlay.Overlay

private class MyLifecycleOwner : SavedStateRegistryOwner {
    private var lifecycleRegistry = LifecycleRegistry(this)
    private var savedStateRegistryController = SavedStateRegistryController.create(this)

    fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    fun performRestore(savedState: Bundle?) =
        savedStateRegistryController.performRestore(savedState)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: ComposeView
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var lifecycleOwner: MyLifecycleOwner

    private val overlayViewModel by lazy { OverlayViewModel() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, indent: Intent?) {
            if (indent?.action == "ru.n08i40k.poco.triggers.intent.TRIGGERS_CLOSED")
                overlayViewModel.close()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()

        startForeground(
            1,
            NotificationCompat
                .Builder(this, "OVERLAY")
                .setContentTitle(getString(R.string.overlay_status_title))
                .setContentText(getString(R.string.overlay_status_desc))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setSilent(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )

        getSystemService(VibratorManager::class.java)
            .vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createWaveform(
                        longArrayOf(100, 100, 100, 100),
                        intArrayOf(255, 200, 170, 150),
                        -1
                    )
                )
            )

        windowManager = getSystemService(WindowManager::class.java)

        viewModelStore = ViewModelStore()
        lifecycleOwner = MyLifecycleOwner()

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore
                    get() = viewModelStore
            })

            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            setContent {
                Overlay(overlayViewModel)
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            horizontalMargin = 0f
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        windowManager.addView(floatingView, layoutParams)

        registerReceiver(
            receiver,
            IntentFilter("ru.n08i40k.poco.triggers.intent.TRIGGERS_CLOSED"),
            RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager.removeView(floatingView)

        unregisterReceiver(receiver)
    }
}