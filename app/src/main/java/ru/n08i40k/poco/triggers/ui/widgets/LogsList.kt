package ru.n08i40k.poco.triggers.ui.widgets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.n08i40k.poco.triggers.daemon.DaemonBridge
import ru.n08i40k.poco.triggers.ui.model.LogsViewModel

@Composable
fun LogsList(modifier: Modifier = Modifier) {
    var logs = remember { mutableStateListOf<String>() }

    val isLoaded by DaemonBridge.initialized.collectAsStateWithLifecycle(false)
    val logsReader = hiltViewModel<LogsViewModel>()

    LaunchedEffect(isLoaded) {
        if (!isLoaded)
            return@LaunchedEffect

        logsReader.attach()
        logsReader.line.collect { logs.add(it) }
    }

    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val scrollState = rememberScrollState()

        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(10.dp)
        ) {
            LazyColumn(Modifier.wrapContentWidth()) {
                items(logs) {
                    Text(
                        it,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        maxLines = 1
                    )
                }
            }
        }
    }
}