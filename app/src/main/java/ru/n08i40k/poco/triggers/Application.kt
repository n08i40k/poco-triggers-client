package ru.n08i40k.poco.triggers

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.daemon.DaemonBridge
import ru.n08i40k.poco.triggers.daemon.DaemonClient
import ru.n08i40k.poco.triggers.model.AppViewModel

@HiltAndroidApp
class Application : Application() {
    companion object {
        private const val TAG = "AOSP-Triggers"

        private lateinit var _INSTANCE: ru.n08i40k.poco.triggers.Application
        val INSTANCE get() = _INSTANCE
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var _initialized = false
    private var _daemonClient: DaemonClient? = null

    /**
     * Whether the application was initialized
     * or whether root permissions were present at the time of the initialization attempt.
     */
    val initialized get() = _initialized

    /**
     * A client to communicate with the daemon via socket.
     */
    val daemonClient get() = _daemonClient

    val viewModel by lazy { AppViewModel(this) }

    override fun onCreate() {
        _INSTANCE = this

        super.onCreate()

        init()
    }

    override fun onTerminate() {
        _daemonClient?.close()

        super.onTerminate()
    }

    fun init() {
        if (initialized)
            return

        if (_daemonClient != null) {
            _daemonClient!!.close()
            _daemonClient = null
        }

        val shell = Shell.Builder.create().build()
        shell.newJob().add("su").exec()

        if (!shell.isRoot)
            return

        grantPermissions(shell)

        DaemonBridge.update()
        DaemonBridge.start()

        coroutineScope.launch {
            Thread.sleep(3000)

            _daemonClient = DaemonClient()
        }

        _initialized = true
    }

    private fun grantPermissions(shell: Shell) {
        Log.i(TAG, "Auto-grant permissions...")

        val packageName = applicationInfo.packageName

        val cmdList = listOf<String>(
            "pm grant $packageName android.permission.SYSTEM_ALERT_WINDOW",
            "pm grant $packageName android.permission.POST_NOTIFICATIONS",
            "pm grant $packageName android.permission.FOREGROUND_SERVICE",
            "appops set $packageName ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $packageName BIND_ACCESSIBILITY_SERVICE allow",
            getUpdateServicesCommand(shell),
        )

        for (cmd in cmdList) {
            Log.i(TAG, "Executing: $cmd")
            shell.newJob().add(cmd).exec()
        }
    }

    /**
     * Getting the command to enable the application's accessibility service.
     */
    private fun getUpdateServicesCommand(shell: Shell): String {
        // Get a list of currently enabled accessibility services in a system.
        val enabledServices = mutableListOf<String>()
        shell
            .newJob()
            .add("settings get secure enabled_accessibility_services")
            .to(enabledServices)
            .exec()

        // Ensure that command runs as expected.
        assert(enabledServices.size == 1)

        // If no accessibility services enabled in a system,
        // the command will return "null" string.
        //
        // But if any accessibility service is enabled,
        // the command will return a set of strings separated by ':'.
        val newEnabledServices = enabledServices[0]
            .let { if (it == "null") mutableSetOf() else enabledServices[0].split(":").toSet() }
            .plus("ru.n08i40k.poco.triggers/.service.AccessibilityService")
            .joinToString(":")

        return "settings put secure enabled_accessibility_services $newEnabledServices"
    }

    fun sendSettings(triggers: Triggers) = _daemonClient?.send(triggers)
}