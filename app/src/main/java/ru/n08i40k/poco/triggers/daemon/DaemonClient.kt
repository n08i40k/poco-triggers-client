package ru.n08i40k.poco.triggers.daemon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.Triggers
import java.net.Socket

internal class DaemonClient {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var socket: Socket? = null

    private fun reconnect() {
        if (socket?.isConnected == true)
            return

        socket?.close()
        socket = Socket("127.0.0.1", 5555)
    }

    fun send(triggers: Triggers) {
        val data = DaemonMessage(
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
        ).toByteArray()

        coroutineScope.launch {
            reconnect()

            socket!!.outputStream.apply {
                write(data)
                flush()
            }
        }
    }

    fun close() {
        coroutineScope.launch {
            socket?.close()
            socket = null
        }
    }
}
