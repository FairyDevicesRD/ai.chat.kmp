package ai.fd.shared.aichat.di

import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelProviders {
    val appViewModel: AppViewModel
}

fun initializeMetro(): AppGraph = createGraph<AppGraph>()
