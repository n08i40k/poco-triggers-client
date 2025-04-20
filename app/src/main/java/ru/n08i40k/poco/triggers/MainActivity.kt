package ru.n08i40k.poco.triggers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.n08i40k.poco.triggers.ui.screen.AppScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "OVERLAY",
            getString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = getString(R.string.overlay_channel_description)

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            createNotificationChannel()
        } catch (_: Exception) {
        }

        setContent {
            AppScreen()
        }
    }
}