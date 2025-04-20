package ru.n08i40k.poco.triggers.ui.model

data class StatusUiState(
    val daemonRunning: Boolean,
    val socketConnected: Boolean,
    val accessibilityService: Boolean,
)