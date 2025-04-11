package ru.n08i40k.poco.triggers.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.ui.overlay.OverlayTriggerData

class OverlayViewModel : ViewModel() {
    private val _state = MutableStateFlow(OverlayUiState.new())
    val state = _state.asStateFlow()

    fun close() {
        _state.update {
            it.copy(waitForClose = true)
        }
    }

    fun updateTriggers(
        upperTrigger: OverlayTriggerData,
        lowerTrigger: OverlayTriggerData,
    ) {
        _state.update {
            it.copy(
                upperTrigger = upperTrigger,
                lowerTrigger = lowerTrigger
            )
        }

        val app = Application.INSTANCE

        val appTriggers = app.triggers
        val overlayTriggers = _state.value

        app.triggers = appTriggers.toBuilder()
            .setUpper(overlayTriggers.upperTrigger.toDataStore(appTriggers.upper.toBuilder()))
            .setLower(overlayTriggers.lowerTrigger.toDataStore(appTriggers.lower.toBuilder()))
            .build()

        viewModelScope.launch {
            app.settings.updateData {
                it
                    .toBuilder()
                    .setTriggers(app.triggers)
                    .build()
            }

            app.sendSetting()
        }
    }
}