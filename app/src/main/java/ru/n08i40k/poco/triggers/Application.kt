package ru.n08i40k.poco.triggers

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.daemon.DaemonBridge
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.daemon.DaemonClient
import ru.n08i40k.poco.triggers.daemon.DaemonMessage

class Application : Application() {
    companion object {
        private const val TAG = "POCO-Triggers"

        private lateinit var _INSTANCE: ru.n08i40k.poco.triggers.Application
        val INSTANCE get() = _INSTANCE
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var daemonClient: DaemonClient? = null

    lateinit var triggers: Triggers

    override fun onCreate() {
        _INSTANCE = this

        super.onCreate()

        coroutineScope.launch {
            triggers = this@Application.settings.data.map { it.triggers }.first()
        }

        init()
    }

    override fun onTerminate() {
        daemonClient?.close()

        super.onTerminate()
    }

    fun init() {
        if (daemonClient != null) {
            daemonClient!!.close()
            daemonClient = null
        }

        val shell = Shell.getShell()
        shell.newJob().add("su").exec()

        if (!shell.isRoot)
            return

        Log.d(TAG, "Auto-grant permissions...")
        shell
            .newJob()
            .add(
                "pm grant ru.n08i40k.poco.triggers android.permission.SYSTEM_ALERT_WINDOW",
                "pm grant ru.n08i40k.poco.triggers android.permission.POST_NOTIFICATIONS",
                "pm grant ru.n08i40k.poco.triggers android.permission.FOREGROUND_SERVICE"
            )
            .exec()

        DaemonBridge.update()
        DaemonBridge.start()

        coroutineScope.launch {
            Thread.sleep(3000)

            daemonClient = DaemonClient()
            sendSetting()
        }
    }

    fun sendSetting() {
        daemonClient?.send(
            DaemonMessage(
                upper = DaemonMessage.Trigger(
                    enabled = triggers.upper.enabled,
                    x = triggers.upper.pos.x * 10,
                    y = triggers.upper.pos.y * 10,
                ),
                lower = DaemonMessage.Trigger(
                    enabled = triggers.lower.enabled,
                    x = triggers.lower.pos.x * 10,
                    y = triggers.lower.pos.y * 10,
                )
            )
        )
    }
}