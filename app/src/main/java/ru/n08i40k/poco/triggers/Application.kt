package ru.n08i40k.poco.triggers

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.proto.settings

class Application : Application() {
    companion object {
        private var instance: ru.n08i40k.poco.triggers.Application? = null

        val applicationContext get() = instance!!.applicationContext
    }

    init {
        instance = this
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    lateinit var triggers: Triggers

    override fun onCreate() {
        super.onCreate()

        coroutineScope.launch {
            triggers = this@Application.settings.data.map { it.triggers }.first()
        }
    }
}