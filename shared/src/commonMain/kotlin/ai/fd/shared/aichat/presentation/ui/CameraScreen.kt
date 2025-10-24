package ai.fd.shared.aichat.presentation.ui

import ai.fd.shared.aichat.Logging
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    modifier: Modifier,
    onClose: () -> Unit,
    onCaptured: (byteArray: ByteArray) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cameraController = remember { mutableStateOf<CameraController?>(null) }
    val isTaking = remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isTaking.value) return@FloatingActionButton
                    isTaking.value = true
                    scope.launch {
                        try {
                            val cameraController = cameraController.value ?: return@launch
                            // 古い機種など一部のデバイスでは最初のフレームが真っ黒になるバグがあるため、１枚目を取得して無視するようにするといい。
                            // cameraController.takeAsync()
                            cameraController
                                .takeAsync()
                                .onSuccess {
                                    onCaptured(it)
                                    onClose()
                                }
                                .onFailure { Logging.e("Camera error: $it") }
                        } finally {
                            isTaking.value = false
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Filled.Camera, contentDescription = "take picture")
            }
        },
    ) { paddingValues ->
        CameraPreview(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            cameraConfiguration = {
                setCameraLens(CameraLens.BACK)
                setFlashMode(FlashMode.OFF)
                setImageFormat(ImageFormat.JPEG)
                setDirectory(Directory.DCIM)
            },
            onCameraControllerReady = { cameraController.value = it },
        )
    }
}

private suspend fun CameraController.takeAsync(): Result<ByteArray> {
    return when (val result = this.takePicture()) {
        is ImageCaptureResult.Success -> {
            Logging.v("success takeAsync")
            Result.success(result.byteArray)
        }
        is ImageCaptureResult.Error -> {
            Logging.e("failed to takeAsync. error: ${result.exception}")
            Result.failure(result.exception)
        }
    }
}
