package ru.n08i40k.poco.triggers.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.Trigger
import ru.n08i40k.poco.triggers.Triggers
import ru.n08i40k.poco.triggers.proto.settings

data class AppState(
    val packageName: String,

    val upperTrigger: Trigger,
    val lowerTrigger: Trigger,
) {
    companion object {
        fun new() =
            AppState(
                packageName = "",
                upperTrigger = Trigger.getDefaultInstance(),
                lowerTrigger = Trigger.getDefaultInstance()
            )
    }
}

class AppViewModel(private val application: Application) : ViewModel() {
    private val _state = MutableStateFlow(AppState.new())
    val state = _state.asStateFlow()

    fun setPackageName(packageName: String?) {
        if (packageName == null) {
            _state.update {
                it.copy(
                    packageName = "",
                    upperTrigger = Trigger.getDefaultInstance(),
                    lowerTrigger = Trigger.getDefaultInstance()
                )
            }

            return
        }

        val triggers =
            runBlocking { application.settings.data.map { it.appsMap[packageName] }.first() }
                ?: Triggers.getDefaultInstance()

        _state.update {
            it.copy(
                packageName = packageName,
                upperTrigger = triggers.upper,
                lowerTrigger = triggers.lower,
            )
        }

        application.sendSettings(triggers)
    }

    fun updateTriggers(upper: Trigger, lower: Trigger) {
        _state.update {
            it.copy(
                upperTrigger = upper,
                lowerTrigger = lower
            )
        }
    }

    fun save() {
        val state = _state.value

        if (state.packageName.isEmpty())
            return

        val triggers = Triggers
            .newBuilder()
            .setUpper(state.upperTrigger)
            .setLower(state.lowerTrigger)
            .build()

        runBlocking {
            application
                .settings
                .updateData {
                    it
                        .toBuilder()
                        .putApps(state.packageName, triggers)
                        .build()
                }
        }

        application.sendSettings(triggers)
    }
}