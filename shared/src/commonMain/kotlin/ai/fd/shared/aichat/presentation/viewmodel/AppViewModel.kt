package ai.fd.shared.aichat.presentation.viewmodel

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.Logging
import ai.fd.shared.aichat.domain.usecase.ConversationUseCase
import ai.fd.shared.aichat.platform.AudioPlayer
import ai.fd.shared.aichat.platform.AudioRecorder
import ai.fd.shared.aichat.platform.JpegEditor
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.unwrapError
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AppViewModel(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val jpegEditor: JpegEditor,
    private val conversationUseCase: ConversationUseCase,
    private val recordCoroutineCtx: CoroutineContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var audioChunks = mutableListOf<ByteArray>()

    init {
        Logging.d("init AppViewModel")
        audioPlayer.setCompletedListener {
            // 再生終了したら
            // thinking -> default に戻す
            _uiState.value =
                _uiState.value.copy(isThinking = false, buttonState = ButtonState.READY)
        }
    }

    fun onRecordButtonClick() {
        if (uiState.value.isThinking) return

        when (uiState.value.buttonState) {
            ButtonState.READY -> startRecording()
            ButtonState.MIC_USING -> stopRecording()
            else -> {}
        }
    }

    fun onCapturedImage(byteArray: ByteArray) {
        val imageBitmap =
            jpegEditor
                .resizeJpeg(jpegBytes = byteArray, maxLongSide = MAX_LONG_SIDE)
                .decodeToImageBitmap()
        _uiState.value = _uiState.value.copy(imageBmp = imageBitmap)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startRecording() {
        // 既に録音中であれば何もしない
        if (recordingJob?.isActive == true) return

        Logging.d("startRecording")

        // 録音中のデータをクリア
        audioChunks.clear()

        // UIを更新して録音開始状態にする
        _uiState.value =
            _uiState.value.copy(
                buttonState = ButtonState.MIC_USING,
                errorMessage = null,
                asrRecognizedText = null,
                aiAnswerText = null,
            )

        recordingJob =
            viewModelScope.launch {
                try {
                    audioRecorder.captureFlow().flowOn(recordCoroutineCtx).collect { audioChunk ->
                        Logging.v("Received audio chunk of size: ${audioChunk.size}")
                        audioChunks.add(audioChunk)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: AudioRecorder.PermissionDeniedException) {
                    handleError(DomainError.OperationError.PermissionDenied)
                } catch (e: Exception) {
                    handleError(DomainError.OperationError.MicError(e))
                } finally {
                    processAudioData()
                }
            }
    }

    private fun stopRecording() {
        Logging.d("stopRecording")
        // Jobをキャンセルして録音を停止
        recordingJob?.cancel()
        recordingJob = null

        // 録音停止後、thinking状態に移行
        _uiState.value = _uiState.value.copy(isThinking = true, buttonState = ButtonState.DISABLED)
    }

    private fun processAudioData() {
        Logging.d("processAudioData")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(asrRecognizedText = null, aiAnswerText = null)
            // 録音結果 -> ASR -> Ai question -> TTS
            val bmp = _uiState.value.imageBmp
            val result =
                conversationUseCase(
                    pcmData = audioChunksToByteArray(),
                    jpeg = if (bmp == null) null else jpegEditor.toJpegByteArray(bmp),
                    callback =
                        object : ConversationUseCase.ConversationCallback {
                            override fun onAsrRecognized(text: String) {
                                _uiState.value = _uiState.value.copy(asrRecognizedText = text)
                            }

                            override fun onAiAnswer(text: String) {
                                _uiState.value = _uiState.value.copy(aiAnswerText = text)
                            }

                            override fun onTtsSynthesized(pcm: ByteArray) {
                                // (TTS) -> 音声再生
                                runCatching { audioPlayer.play(pcm) }
                                    .onFailure {
                                        handleError(DomainError.OperationError.PlayerError(it))
                                        audioPlayer.stop()
                                    }
                            }
                        },
                )

            if (result.isErr) handleError(result.unwrapError())
        }
    }

    private fun handleError(e: DomainError) {
        val message =
            when (e) {
                is DomainError.GetTokenError -> "Failed to connect to the server"
                is DomainError.AsrError -> "Failed to recognize speech"
                is DomainError.EmptyAsrError -> "Recognized speech result is empty"
                is DomainError.TtsError -> "Failed to synthesize speech"
                is DomainError.AiAgentError -> "Failed to communicate with the AI agent"
                is DomainError.OperationError.PermissionDenied -> "Permission denied"
                is DomainError.OperationError.PlayerError -> "Failed to play audio"
                is DomainError.OperationError.MicError -> "Failed to record audio"
            // else -> "Unknown error"
            }
        Logging.d("handleError: $message")
        _uiState.value =
            _uiState.value.copy(
                isThinking = false,
                buttonState = ButtonState.READY,
                errorMessage = message,
            )
    }

    private fun audioChunksToByteArray(): ByteArray {
        // 録音したデータを1つにする
        return audioChunks.fold(byteArrayOf()) { acc, array -> acc + array }
    }

    private companion object {
        const val MAX_LONG_SIDE = 800
    }
}
