package ru.n08i40k.poco.triggers.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.n08i40k.poco.triggers.daemon.DaemonBridge
import java.io.RandomAccessFile
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService

class LogsViewModel : ViewModel() {
    private var attached = false

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val file = DaemonBridge.logFile
    private val randomAccessFile = RandomAccessFile(file, "r")

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKey: WatchKey =
        file.toPath().parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

    private var _line = MutableSharedFlow<String>(128)
    val line = _line.asSharedFlow()

    private suspend fun read() {
        randomAccessFile.fd.sync()

        while (randomAccessFile.filePointer != randomAccessFile.length())
            _line.emit(randomAccessFile.readLine())
    }

    fun attach() {
        if (attached)
            return

        attached = true

        runBlocking { read() }

        coroutineScope.launch {
            var key: WatchKey? = null

            do {
                key = watchService.take()

                if (key == null)
                    break

                for (event in watchKey.pollEvents()) {
                    if ((event.context() as Path).toString() != file.name)
                        continue

                    read()
                }

                key.reset()
            } while (true)
        }
    }
}