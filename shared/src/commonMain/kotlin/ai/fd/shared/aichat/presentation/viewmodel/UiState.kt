package ai.fd.shared.aichat.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap

data class UiState(
    /** AI考え中 */
    val isThinking: Boolean = false,

    /** ボタンの状態 */
    val buttonState: ButtonState = ButtonState.READY,

    /** エラーメッセージ */
    val errorMessage: String? = null,

    /** 撮影した画像 */
    val imageBmp: ImageBitmap? = null,

    /** ASRで認識したテキスト */
    val asrRecognizedText: String? = null,

    /** AIで生成したテキスト */
    val aiAnswerText: String? = null,
)

enum class ButtonState {
    READY,
    MIC_USING,
    DISABLED,
}
