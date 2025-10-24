package ai.fd.droid.aichat

import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import ai.fd.shared.aichat.presentation.viewmodel.ButtonState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import kotlinx.coroutines.flow.Flow

@Composable
fun ThinkletApp(
    viewModel: AppViewModel,
    eventFlow: Flow<Unit>,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraController = remember { mutableStateOf<CameraController?>(null) }

    LaunchedEffect(Unit) {
        eventFlow.collect {
            if (uiState.isThinking) return@collect
            if (cameraController.value == null) return@collect

            when (uiState.buttonState) {
                ButtonState.READY -> {
                    // 録音開始
                    onRecordStart()
                    viewModel.onRecordButtonClick()
                }

                ButtonState.MIC_USING -> {
                    onRecordStop()
                    // 撮影
                    val result = cameraController.value?.takePicture()
                    if (result is ImageCaptureResult.Success) {
                        viewModel.onCapturedImage(result.byteArray)
                    }
                    // 録音停止 -> THINKING
                    viewModel.onRecordButtonClick()
                }

                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier.systemGesturesPadding(),
        bottomBar = {
            BottomAppBar(
                modifier =
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (uiState.isThinking) Text("AI thinking...")
                    else
                        when (uiState.buttonState) {
                            ButtonState.READY -> Text("Ready")
                            ButtonState.MIC_USING -> Text("Recording...")
                            else -> {}
                        }
                }
            }
        },
    ) { paddingValues ->
        CameraPreview(
            modifier = Modifier.padding(paddingValues),
            onSetup = { cameraController.value = it },
        )
    }
}

@Composable
fun CameraPreview(modifier: Modifier, onSetup: (cameraController: CameraController) -> Unit) {
    CameraPreview(
        modifier = modifier,
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.OFF)
            setImageFormat(ImageFormat.JPEG)
            setDirectory(Directory.DCIM)
        },
        onCameraControllerReady = { onSetup(it) },
    )
}
