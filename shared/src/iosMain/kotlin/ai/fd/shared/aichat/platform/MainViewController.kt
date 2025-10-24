package ai.fd.shared.aichat.platform

import ai.fd.shared.aichat.di.AppGraph
import ai.fd.shared.aichat.di.initializeMetro
import ai.fd.shared.aichat.presentation.ui.App
import androidx.compose.ui.window.ComposeUIViewController

object AppGraphStore {
    fun createAppGraph(): AppGraph {
        return initializeMetro()
    }
}

fun MainViewController(appGraph: AppGraph) = ComposeUIViewController { App(appGraph.appViewModel) }
