package ru.n08i40k.poco.triggers.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.n08i40k.poco.triggers.R
import ru.n08i40k.poco.triggers.ui.model.StatusUiState
import ru.n08i40k.poco.triggers.ui.model.StatusViewModel
import ru.n08i40k.poco.triggers.ui.widgets.FAQDialog
import ru.n08i40k.poco.triggers.ui.widgets.FAQInfo
import ru.n08i40k.poco.triggers.ui.widgets.LogsList

@Composable
private fun StatusButton(name: String, ok: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick,
        border = ButtonDefaults.outlinedButtonBorder()
            .copy(brush = SolidColor(if (ok) Color.Green else Color.Red))
    ) { Text(name) }
}

private enum class FAQ {
    Daemon,
    DaemonClient,
    AccessibilityService
}

private val faqMap = mapOf<FAQ, FAQInfo>(
    FAQ.Daemon to FAQInfo(
        R.string.faq_daemon_title,
        R.string.faq_daemon_description
    ),
    FAQ.DaemonClient to FAQInfo(
        R.string.faq_daemon_client_title,
        R.string.faq_daemon_client_description
    ),
    FAQ.AccessibilityService to FAQInfo(
        R.string.faq_aservice_title,
        R.string.faq_aservice_description
    ),
)

@Composable
fun StatusScreen() {
    val viewModel = hiltViewModel<StatusViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle(StatusUiState(true, true, true))

    var currentFAQ by remember { mutableStateOf<FAQ?>(null) }

    currentFAQ?.let { FAQDialog(faqMap[it]!!) { currentFAQ = null } }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceAround) {
            StatusButton(
                stringResource(R.string.status_button_daemon),
                state.daemonRunning
            ) { currentFAQ = FAQ.Daemon }

            StatusButton(
                stringResource(R.string.status_button_socket),
                state.socketConnected
            ) { currentFAQ = FAQ.DaemonClient }

            StatusButton(
                stringResource(R.string.status_button_accessibility),
                state.accessibilityService
            ) { currentFAQ = FAQ.AccessibilityService }
        }

        LogsList(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
        )
    }
}