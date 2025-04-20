package ru.n08i40k.poco.triggers.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.R
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.ui.theme.AppTheme

@Preview(showSystemUi = true)
@Composable
private fun NoRootScreenPreview() {
    AppTheme {
        Surface {
            NoRootScreen()
        }
    }
}

@Composable
fun NoRootScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val onClick: () -> Unit = {
        val shell = Shell.Builder.create().build()
        shell.newJob().add("su").exec()

        coroutineScope.launch {
            if (shell.isRoot) {
                context.settings.updateData {
                    it
                        .toBuilder()
                        .setCompletedIntro(true)
                        .build()
                }

                shell
                    .newJob()
                    .add("am force-stop ${Application.INSTANCE.applicationInfo.packageName}")
                    .exec()
            } else snackbarHostState.showSnackbar(context.getString(R.string.no_root_again))
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(it), Alignment.Center
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Settings,
                    "No Root",
                    Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Column(
                    Modifier.padding(40.dp, 40.dp),
                    Arrangement.spacedBy(10.dp),
                    Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.no_root_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        stringResource(R.string.no_root_text),
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                Alignment.BottomCenter
            ) {
                Button(
                    onClick,
                    Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.try_again)) }
            }
        }
    }
}
