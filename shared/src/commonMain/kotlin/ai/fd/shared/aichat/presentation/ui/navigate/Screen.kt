package ai.fd.shared.aichat.presentation.ui.navigate

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Home : Screen

    @Serializable data object Camera : Screen
}
