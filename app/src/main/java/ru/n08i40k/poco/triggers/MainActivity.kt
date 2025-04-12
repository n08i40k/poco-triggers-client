package ru.n08i40k.poco.triggers

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.n08i40k.poco.triggers.ui.theme.AppTheme
import kotlin.system.exitProcess

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
            AlertDialog
                .Builder(this)
                .setTitle(getString(R.string.root_required_title))
                .setMessage(getString(R.string.root_required_message))
                .setNeutralButton(getString(R.string.exit)) { _, _ -> exitProcess(1) }
                .show()
        }

        setContent {
            AppTheme {
                Surface {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp), Alignment.Center
                    ) {
                        Text(stringResource(R.string.app_main_description))
                    }
                }
            }
        }
    }
}