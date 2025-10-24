package ai.fd.droid.aichat

import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.lifecycle.ProcessCameraProvider

/** CameraX向けのTHINKLETの高速化パッチ */
object CameraXPatch {
    private var patched: Boolean = false

    fun apply() {
        if (!patched && Build.MODEL.contains("THINKLET")) {
            ProcessCameraProvider.configureInstance(
                CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                    .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
                    // .setMinimumLoggingLevel(Log.WARN)
                    .build()
            )
            patched = true
        }
    }
}
