package ru.n08i40k.poco.triggers.daemon

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.BuildConfig
import java.io.File

object DaemonBridge {
    /**
     * Name of daemon's executable file.
     */
    const val EXECUTABLE_NAME = "aosp-triggers-daemon"

    /**
     * Name of daemon's unpacked executable file.
     */
    const val EXECUTABLE_NAME_VERSIONED = "$EXECUTABLE_NAME+${BuildConfig.BUILD_TIME}"

    /**
     * Full path to daemon's executable file.
     */
    const val EXECUTABLE_PATH = "/data/local/tmp/$EXECUTABLE_NAME_VERSIONED"

    /**
     * Full path to daemon's log file.
     */
    val LOG_PATH = Application.INSTANCE.filesDir.path + "/daemon.log"

    /**
     * Get daemon's executable file
     */
    val executableFile get() = File(EXECUTABLE_PATH)

    /**
     * Get daemon's log file.
     */
    val logFile get() = File(LOG_PATH)

    /**
     * Get pid of daemon if it's running.
     */
    val pid: Int?
        get() {
            val output =
                Shell.cmd("ps -A | grep $EXECUTABLE_NAME_VERSIONED | awk '{print $2}'").exec().out

            if (output.isEmpty() || output[0].isEmpty())
                return null

            return output.getOrNull(0)?.toIntOrNull()
        }

    /**
     * Check if the daemon is started.
     */
    val started get() = pid != null

    private val _initialized = MutableStateFlow<Boolean>(false)

    /**
     * State-flow indicating whether an attempt was made to stop/start the daemon.
     */
    val initialized = _initialized.asStateFlow()

    /**
     * Replace old daemon executable with its new version.
     */
    fun update(force: Boolean = false) {
        if (!force && executableFile.exists())
            return

        stop()

        val baseApk = Application.INSTANCE.applicationInfo.sourceDir

        Shell
            .getShell()
            .newJob()
            .add(
                // Remove all daemon's files and logs
                "rm -f /data/local/tmp/$EXECUTABLE_NAME*",
                // Extract a new version
                "unzip -p $baseApk assets/bin/$EXECUTABLE_NAME > $EXECUTABLE_PATH",
                // Allow executing a file
                "chmod +x $EXECUTABLE_PATH",
            ).exec()
    }

    /**
     * Stop currently running daemon.
     */
    fun stop() {
        Shell.cmd("pkill -f $EXECUTABLE_NAME").exec()

        _initialized.update { false }
    }

    /**
     * Start daemon if not started.
     *
     * If force is true, the daemon will be restarted.
     */
    fun start(force: Boolean = false) {
        if (force)
            stop()
        else if (started) {
            _initialized.update { true }
            return
        }

        Shell
            .getShell()
            .newJob()
            .add(
                "rm $LOG_PATH && touch $LOG_PATH",
                "$EXECUTABLE_PATH &> $LOG_PATH"
            )
            .submit()

        _initialized.update { true }
    }
}