package ai.fd.droid.aichat

import ai.fd.shared.aichat.di.AppGraph
import ai.fd.shared.aichat.di.initializeMetro
import ai.fd.shared.aichat.presentation.ui.App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private val appGraph: AppGraph by lazy { initializeMetro() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // sharedモジュールで定義されたApp Composableを呼び出す
            App(appGraph.appViewModel)
        }
    }
}
