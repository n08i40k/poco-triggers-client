package ru.n08i40k.poco.triggers.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.daemon.DaemonBridge
import ru.n08i40k.poco.triggers.service.AccessibilityService

class StatusViewModel : ViewModel() {
    private var _state: Flow<StatusUiState>
    val state get() = _state

    init {
        val app = Application.INSTANCE

        _state = flow {
            while (true) {
                emit(
                    StatusUiState(
                        DaemonBridge.started,
                        app.daemonClient?.isConnected == true,
                        AccessibilityService.connected.value
                    )
                )

                delay(1500)
            }
        }.distinctUntilChanged()
    }
}