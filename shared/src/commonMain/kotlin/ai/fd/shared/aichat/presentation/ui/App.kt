package ai.fd.shared.aichat.presentation.ui

import ai.fd.shared.aichat.presentation.ui.navigate.AppNavHost
import ai.fd.shared.aichat.presentation.ui.navigate.Screen
import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

@Composable
fun App(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        startDestination = Screen.Home,
        viewModel = viewModel,
        modifier = modifier,
    )
}
