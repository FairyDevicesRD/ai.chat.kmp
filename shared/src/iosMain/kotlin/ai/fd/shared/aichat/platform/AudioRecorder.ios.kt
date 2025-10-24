package ai.fd.shared.aichat.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import platform.AVFAudio.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
private class AudioRecorderIOS : AudioRecorder {
    private var recordingJob: Job? = null
    private var audioEngine: AVAudioEngine? = null
    private var inputNode: AVAudioInputNode? = null
    private var audioConverter: AVAudioConverter? = null
    private var isRecording = false

    // 目標フォーマット：16bit, 16kHz, 1ch
    private val targetSampleRate = 16000.0
    private val targetChannels = 1u

    override fun captureFlow(): Flow<ByteArray> = callbackFlow {
        recordingJob = coroutineContext[Job]

        try {
            configureAudioSession()

            val engine = AVAudioEngine()
            val input = engine.inputNode
            val inputFormat = input.outputFormatForBus(0u)

            // ターゲットフォーマット（16bit, 16kHz, 1ch）を作成
            val targetFormat =
                AVAudioFormat(
                    commonFormat = AVAudioPCMFormatInt16,
                    sampleRate = targetSampleRate,
                    channels = targetChannels,
                    interleaved = true,
                )

            // フォーマット変換器を作成
            val converter = AVAudioConverter(inputFormat, targetFormat)

            audioEngine = engine
            inputNode = input
            audioConverter = converter
            isRecording = true

            Logging.d("🎤 Input format: ${inputFormat.sampleRate}Hz, ${inputFormat.channelCount}ch")
            Logging.d(
                "🎤 Target format: ${targetFormat.sampleRate}Hz, ${targetFormat.channelCount}ch"
            )

            // 録音用のタップを設置
            input.installTapOnBus(bus = 0u, bufferSize = 1024u, format = inputFormat) { buffer, _ ->
                if (!isRecording || recordingJob?.isActive != true) {
                    return@installTapOnBus
                }

                buffer?.let { audioBuffer ->
                    convertAndSendAudio(audioBuffer, converter, targetFormat) { convertedData ->
                        if (isRecording && convertedData.isNotEmpty()) {
                            Logging.d("🎤 Converted audio: ${convertedData.size} bytes")
                            trySend(convertedData)
                        }
                    }
                }
            }

            startAudioEngine(engine)
        } catch (e: Exception) {
            Logging.d("🎤 AudioRecorder error: ${e.message}")
            close(e)
            return@callbackFlow
        }

        awaitClose { stopRecording() }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun configureAudioSession() {
        if (
            AVAudioSession.sharedInstance().recordPermission() !=
                AVAudioSessionRecordPermissionGranted
        ) {
            throw AudioRecorder.PermissionDeniedException("Microphone permission not granted")
        }

        val audioSession = AVAudioSession.sharedInstance()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()

            // 録音用カテゴリ
            audioSession.setCategory(AVAudioSessionCategoryRecord, error.ptr)

            // 音声認識に最適なモード
            audioSession.setMode(AVAudioSessionModeMeasurement, error.ptr)

            // 16kHzを要求（デバイスが対応していれば）
            audioSession.setPreferredSampleRate(targetSampleRate, error.ptr)

            // セッションをアクティブに
            audioSession.setActive(true, error.ptr)

            error.value?.let { Logging.d("🎤 Audio session warning: ${it.localizedDescription}") }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun convertAndSendAudio(
        inputBuffer: AVAudioPCMBuffer,
        converter: AVAudioConverter,
        targetFormat: AVAudioFormat,
        onDataReady: (ByteArray) -> Unit,
    ) {
        val inputFrameCount = inputBuffer.frameLength
        if (inputFrameCount == 0u) return

        // 変換後のフレーム数を推定（サンプルレート変換考慮）
        val outputFrameCount =
            (inputFrameCount.toDouble() * targetFormat.sampleRate / inputBuffer.format.sampleRate)
                .toUInt()

        val outputBuffer = AVAudioPCMBuffer(targetFormat, outputFrameCount)

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            var inputConsumed = false

            converter.convertToBuffer(outputBuffer, error.ptr) { _, outStatus ->
                if (!inputConsumed) {
                    inputConsumed = true
                    outStatus?.pointed?.value = 0L // noErr
                    inputBuffer
                } else {
                    outStatus?.pointed?.value = 1L // end of stream
                    null
                }
            }

            error.value?.let { err ->
                Logging.d("🎤 Conversion error: ${err.localizedDescription}")
                return@memScoped
            }

            // 変換されたデータを抽出
            if (outputBuffer.frameLength > 0u) {
                extractPCMData(outputBuffer, onDataReady)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun extractPCMData(buffer: AVAudioPCMBuffer, onDataReady: (ByteArray) -> Unit) {
        buffer.audioBufferList?.let { audioBufferList ->
            val audioBuffer = audioBufferList.pointed.mBuffers.pointed
            val dataPtr = audioBuffer.mData
            val dataSize = audioBuffer.mDataByteSize.toInt()

            if (dataPtr != null && dataSize > 0) {
                try {
                    // 既に16bitデータなので、そのままコピー
                    val byteArray = ByteArray(dataSize)
                    byteArray.usePinned { pinned ->
                        platform.posix.memcpy(pinned.addressOf(0), dataPtr, dataSize.convert())
                    }
                    onDataReady(byteArray)
                } catch (e: Exception) {
                    Logging.d("🎤 Failed to extract PCM data: ${e.message}")
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun startAudioEngine(engine: AVAudioEngine) {
        engine.prepare()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val success = engine.startAndReturnError(error.ptr)

            if (!success) {
                error.value?.let { err ->
                    Logging.d("🎤 Failed to start engine: ${err.localizedDescription}")
                    throw IllegalStateException(
                        "Cannot start audio engine: ${err.localizedDescription}"
                    )
                }
                throw IllegalStateException("Cannot start audio engine")
            }
        }

        Logging.d("🎤 Audio engine started successfully")
    }

    private fun stopRecording() {
        Logging.d("🎤 Stopping audio recording...")
        isRecording = false
        recordingJob?.cancel()
        cleanup()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cleanup() {
        try {
            audioEngine?.let { engine ->
                if (engine.running) {
                    engine.stop()
                }

                inputNode?.let { node ->
                    try {
                        node.removeTapOnBus(0u)
                    } catch (e: Exception) {
                        Logging.d("🎤 Warning: Failed to remove tap: ${e.message}")
                    }
                }
            }

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                AVAudioSession.sharedInstance().setActive(false, error.ptr)
            }
        } catch (e: Exception) {
            Logging.d("🎤 Error during cleanup: ${e.message}")
        } finally {
            audioEngine = null
            inputNode = null
            audioConverter = null
            recordingJob = null
            Logging.d("🎤 Audio recorder cleanup completed")
        }
    }
}

actual fun createAudioRecorder(): AudioRecorder = AudioRecorderIOS()
