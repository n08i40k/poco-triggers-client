package ru.n08i40k.poco.triggers.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.ui.theme.AppTheme

@Composable
private fun AppInnerScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val nav: (from: String, to: String) -> Unit =
        { from, to -> navController.navigate(to) { popUpTo(from) { inclusive = true } } }

    val showHello = runBlocking { !context.settings.data.map { it.completedIntro }.first() }
    val nonRoot = !Application.INSTANCE.initialized

    NavHost(
        navController,
        if (showHello)
            "hello"
        else if (nonRoot)
            "no-root"
        else
            "status",
        Modifier.fillMaxSize()
    ) {
        composable("hello") {
            HelloScreen {
                if (nonRoot)
                    nav("hello", "no-root")
                else
                    nav("hello", "status")
            }
        }

        composable("no-root") { NoRootScreen() }

        composable("status") { StatusScreen() }
    }
}

@Composable
fun AppScreen() {
    AppTheme {
        Surface {
            Box(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Vertical))) {
                AppInnerScreen()
            }
        }
    }
}