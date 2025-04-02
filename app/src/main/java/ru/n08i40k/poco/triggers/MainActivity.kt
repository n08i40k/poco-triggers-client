package ru.n08i40k.poco.triggers

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
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
import com.topjohnwu.superuser.Shell
import ru.n08i40k.poco.triggers.ui.theme.AppTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shell = Shell.getShell()
        shell.newJob().add("su").exec()

        if (!shell.isRoot) {
            AlertDialog
                .Builder(this)
                .setTitle(getString(R.string.root_required_title))
                .setMessage(getString(R.string.root_required_message))
                .setNeutralButton(getString(R.string.exit)) { _, _ -> exitProcess(1) }
                .show()
        } else {
            Log.d("POCO-Triggers", "Auto-enable accessibility service using root-privileges...")
            Shell
                .cmd("settings put secure enabled_accessibility_services ru.n08i40k.poco.triggers/ru.n08i40k.poco.triggers.service.KeyInterceptorService")
                .exec()

            Log.d("POCO-Triggers", "Auto-grant draw over other apps permission...")
            Shell
                .cmd("pm grant ru.n08i40k.poco.triggers android.permission.SYSTEM_ALERT_WINDOW")
                .exec()
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