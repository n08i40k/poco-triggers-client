package ru.n08i40k.poco.triggers.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.ui.overlay.OverlayTriggerData

class OverlayViewModel : ViewModel() {
    private val _state = MutableStateFlow(OverlayUiState.new())
    val state = _state.asStateFlow()

    fun close() {
        _state.update {
            it.copy(waitForClose = true)
        }
    }

    fun sync() {
        val appViewModel = Application.INSTANCE.viewModel
        val appState = appViewModel.state.value

        _state.update {
            it.copy(
                packageName = appState.packageName,
                upperTrigger = OverlayTriggerData.Companion.fromDataStore(appState.upperTrigger),
                lowerTrigger = OverlayTriggerData.Companion.fromDataStore(appState.lowerTrigger)
            )
        }
    }

    fun updateTriggers(
        upper: OverlayTriggerData,
        lower: OverlayTriggerData,
    ) {
        if (_state.value.packageName?.isNotEmpty() != true)
            return

        _state.update {
            it.copy(
                upperTrigger = upper,
                lowerTrigger = lower
            )
        }

        val appViewModel = Application.INSTANCE.viewModel
        val appState = appViewModel.state.value

        appViewModel.updateTriggers(
            _state.value.upperTrigger.toDataStore(appState.upperTrigger.toBuilder()),
            _state.value.lowerTrigger.toDataStore(appState.lowerTrigger.toBuilder())
        )

        appViewModel.save()
    }
}