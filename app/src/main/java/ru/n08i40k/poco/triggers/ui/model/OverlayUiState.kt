package ru.n08i40k.poco.triggers.ui.model

import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.ui.overlay.OverlayTriggerData

data class OverlayUiState(
    val waitForClose: Boolean,

    val upperTrigger: OverlayTriggerData,
    val lowerTrigger: OverlayTriggerData,
) {
    companion object {
        fun new() = OverlayUiState(
            waitForClose = false,
            upperTrigger = OverlayTriggerData.Companion.fromDataStore(Application.Companion.INSTANCE.triggers.upper),
            lowerTrigger = OverlayTriggerData.Companion.fromDataStore(Application.Companion.INSTANCE.triggers.lower)
        )
    }
}