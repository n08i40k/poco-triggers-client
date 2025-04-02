package ru.n08i40k.poco.triggers.utility

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.Application.Companion.applicationContext
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val DAEMON_PATH = "/data/local/tmp/poco-triggers-daemon"

private data class DaemonMessage(
    val trigger: Short,
    val touch: Short,
    val x: Int,
    val y: Int,
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(12)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(trigger)
        buffer.putShort(touch)
        buffer.putInt(x)
        buffer.putInt(y)

        return buffer.array()
    }
}

private class DaemonClient {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val socket by lazy { Socket("127.0.0.1", 5555) }

    fun send(message: DaemonMessage) {
        val data = message.toByteArray()

        coroutineScope.launch {
            socket.outputStream.apply {
                write(data)
                flush()
            }
        }
    }

    fun close() {
        coroutineScope.launch {
            socket.close()
        }
    }
}

class TouchEmulator {
    val daemonShell by lazy {
        val shell = Shell.Builder.create()
            .setFlags(Shell.FLAG_MOUNT_MASTER)
            .setTimeout(Long.MAX_VALUE)
            .build()

        shell
    }

    private val daemon by lazy {
        val baseApk = applicationContext.applicationInfo.sourceDir

        daemonShell.newJob().add(
            "rm -f /data/local/tmp/poco-triggers-daemon",
            "unzip -p $baseApk assets/bin/poco-triggers-daemon > /data/local/tmp/poco-triggers-daemon",
            "chmod +x /data/local/tmp/poco-triggers-daemon"
        ).exec()

        daemonShell.newJob().add(DAEMON_PATH).submit()
        Thread.sleep(2_000)
    }

    private val daemonClient by lazy {
        daemon

        DaemonClient()
    }

    companion object {
        const val DPI = 10 // event device width and height multiplier
    }

    fun beginTouch(index: Int, x: Int, y: Int) {
        daemonClient.send(DaemonMessage(index.toShort(), 1, x * DPI, y * DPI))
    }

    fun endTouch(index: Int) {
        daemonClient.send(DaemonMessage(index.toShort(), 0, 0, 0))
    }

    fun shutdown() {
        daemonClient.close()
        daemonShell.close()
    }
}