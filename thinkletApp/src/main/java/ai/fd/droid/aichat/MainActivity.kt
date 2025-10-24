package ai.fd.droid.aichat

import ai.fd.shared.aichat.di.AppGraph
import ai.fd.shared.aichat.di.initializeMetro
import android.media.MediaActionSound
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val appGraph: AppGraph by lazy { initializeMetro() }
    private val keyEventChannel: Channel<Unit> = Channel()

    private val mediaActionSound: MediaActionSound by lazy {
        MediaActionSound().apply {
            load(MediaActionSound.START_VIDEO_RECORDING)
            load(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CameraXPatch.apply()
        setContent {
            ThinkletApp(
                viewModel = appGraph.appViewModel,
                eventFlow = keyEventChannel.receiveAsFlow(),
                onRecordStart = { mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING) },
                onRecordStop = { mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING) },
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            keyEventChannel.trySend(Unit)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
