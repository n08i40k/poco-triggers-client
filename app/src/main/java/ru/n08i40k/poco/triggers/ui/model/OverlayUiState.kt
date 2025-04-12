package ru.n08i40k.poco.triggers.ui.model

import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.ui.overlay.OverlayTriggerData

data class OverlayUiState(
    val waitForClose: Boolean,

    val packageName: String?,

    val upperTrigger: OverlayTriggerData,
    val lowerTrigger: OverlayTriggerData,
) {
    companion object {
        fun new(): OverlayUiState {
            val appViewModel = Application.INSTANCE.viewModel
            val data = appViewModel.state.value

            return OverlayUiState(
                waitForClose = false,
                packageName = null,
                upperTrigger = OverlayTriggerData.Companion.fromDataStore(data.upperTrigger),
                lowerTrigger = OverlayTriggerData.Companion.fromDataStore(data.lowerTrigger)
            )
        }
    }
}