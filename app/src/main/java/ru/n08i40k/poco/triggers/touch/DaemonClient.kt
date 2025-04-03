package ru.n08i40k.poco.triggers.touch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

internal class DaemonClient {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val socket by lazy {
        Socket("127.0.0.1", 5555)
    }

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
