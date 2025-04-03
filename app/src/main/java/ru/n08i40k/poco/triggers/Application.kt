package ru.n08i40k.poco.triggers

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.touch.DaemonClient
import ru.n08i40k.poco.triggers.touch.DaemonMessage
import ru.n08i40k.poco.triggers.touch.TriggerSetting

class Application : Application() {
    companion object {
        private const val TAG = "POCO-Triggers"
        private const val DAEMON_PATH = "/data/local/tmp/poco-triggers-daemon"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var daemonShell: Shell? = null
    private var daemonClient: DaemonClient? = null

    lateinit var triggers: Triggers

    override fun onCreate() {
        super.onCreate()

        coroutineScope.launch {
            triggers = this@Application.settings.data.map { it.triggers }.first()
        }

        init()
    }

    override fun onTerminate() {
        daemonClient?.close()
        daemonShell?.close()

        super.onTerminate()
    }

    fun init() {
        if (daemonClient != null) {
            daemonClient!!.close()
            daemonClient = null
        }

        if (daemonShell != null) {
            daemonShell!!.close()
            daemonShell = null
        }

        val shell = Shell.getShell()
        shell.newJob().add("su").exec()

        if (!shell.isRoot)
            return

        Log.d(TAG, "Auto-grant draw over other apps permission...")
        Shell
            .cmd("pm grant ru.n08i40k.poco.triggers android.permission.SYSTEM_ALERT_WINDOW")
            .exec()

        Log.d(TAG, "Auto-grant post notifications permission...")
        Shell
            .cmd("pm grant ru.n08i40k.poco.triggers android.permission.POST_NOTIFICATIONS")
            .exec()

        Log.d(TAG, "Auto-grant foreground service permission...")
        Shell
            .cmd("pm grant ru.n08i40k.poco.triggers android.permission.FOREGROUND_SERVICE")
            .exec()

        daemonShell = {
            val shell = Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(Long.MAX_VALUE)
                .build()

            val baseApk = applicationInfo.sourceDir

            val outHandler = object : ArrayList<String>() {
                override fun add(e: String): Boolean {
                    Log.i("TriggersDaemon", e)
                    return super.add(e)
                }

                override fun addAll(c: Collection<String>): Boolean {
                    c.forEach { Log.i("TriggersDaemon", it) }
                    return super.addAll(c)
                }
            }

            val errHandler = object : ArrayList<String>() {
                override fun add(e: String): Boolean {
                    Log.e("TriggersDaemon", e)
                    return super.add(e)
                }

                override fun addAll(c: Collection<String>): Boolean {
                    c.forEach { Log.e("TriggersDaemon", it) }
                    return super.addAll(c)
                }
            }

            shell.newJob().add(
                "pkill -f poco-triggers-daemon",
                "rm -f /data/local/tmp/poco-triggers-daemon",
                "unzip -p $baseApk assets/bin/poco-triggers-daemon > $DAEMON_PATH",
                "chmod +x /data/local/tmp/poco-triggers-daemon",
                "nohup setsid $DAEMON_PATH > /data/local/tmp/poco-logs &"
            ).to(outHandler, errHandler).exec()

            shell
        }.invoke()

        coroutineScope.launch {
            Thread.sleep(3000)

            daemonClient = DaemonClient()
            sendSetting()
        }
    }

    fun sendSetting() {
        daemonClient?.send(
            DaemonMessage(
                upper = TriggerSetting(
                    index = 0,
                    enabled = triggers.upper.enabled,
                    x = triggers.upper.pos.x * 10,
                    y = triggers.upper.pos.y * 10,
                ),
                lower = TriggerSetting(
                    index = 1,
                    enabled = triggers.lower.enabled,
                    x = triggers.lower.pos.x * 10,
                    y = triggers.lower.pos.y * 10,
                )
            )
        )
    }
}