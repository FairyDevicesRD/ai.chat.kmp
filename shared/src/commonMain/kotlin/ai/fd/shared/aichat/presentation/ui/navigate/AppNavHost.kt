package ai.fd.shared.aichat.presentation.ui.navigate

import ai.fd.shared.aichat.presentation.ui.CameraScreen
import ai.fd.shared.aichat.presentation.ui.HomeScreen
import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Screen,
    viewModel: AppViewModel,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        appGraph(modifier = modifier, navController = navController, viewModel = viewModel)
    }
}

fun NavGraphBuilder.appGraph(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: AppViewModel,
) {
    composable<Screen.Home> {
        HomeScreen(
            modifier = modifier,
            viewModel = viewModel,
            onNavigateToCamera = { navController.navigate(Screen.Camera) },
        )
    }
    composable<Screen.Camera> {
        CameraScreen(
            modifier = modifier,
            onClose = navController::popBackStack,
            onCaptured = viewModel::onCapturedImage,
        )
    }
}
