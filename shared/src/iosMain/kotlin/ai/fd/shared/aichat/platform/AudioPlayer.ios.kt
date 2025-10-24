package ai.fd.shared.aichat.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private class IOSAudioPlayer(private val playerScope: CoroutineScope) : AudioPlayer {

    private val sampleRate = 16000.0
    private val channelCount = 1u
    private val bitDepth = 16
    private val bytesPerSample = bitDepth / 8 * channelCount.toInt() // 2 bytes per sample

    private var audioEngine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var audioFormat: AVAudioFormat? = null

    private val playbackQueue = mutableListOf<ByteArray>()
    private var playbackJob: Job? = null
    private var completedListener: (() -> Unit)? = null

    override fun play(pcmData: ByteArray) {
        Logging.d("🔊 play() called with ${pcmData.size} bytes. Queue size: ${playbackQueue.size}")

        playbackQueue.add(pcmData)

        if (playbackJob?.isActive != true) {
            Logging.d("🔊 Starting new playback job")
            startPlayback()
        } else {
            Logging.d("🔊 Playback job already active, added to queue")
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()

        playbackJob =
            playerScope.launch {
                try {
                    setupAudioSession()
                    setupAudioEngine()

                    while (playbackQueue.isNotEmpty() && isActive) {
                        val data = playbackQueue.removeFirstOrNull() ?: break
                        Logging.d(
                            "🔊 Processing chunk: ${data.size} bytes, remaining in queue: ${playbackQueue.size}"
                        )

                        scheduleAudioBuffer(data)

                        // バッファ処理の待機時間を調整
                        val durationMs = (data.size / bytesPerSample) * 1000 / sampleRate.toInt()
                        Logging.d("🔊 Chunk duration: ${durationMs}ms, waiting...")
                        delay(durationMs.toLong()) // 実際の音声時間分待機
                    }

                    Logging.d("🔊 All chunks processed, waiting for final playback...")
                    delay(1000) // 最後のバッファ再生完了を待機
                    completedListener?.invoke()
                } catch (e: Exception) {
                    Logging.d("🔊 Playback error: ${e.message}")
                } finally {
                    withContext(NonCancellable) { cleanup() }
                }
            }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupAudioSession() {
        val audioSession = AVAudioSession.sharedInstance()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()

            audioSession.setCategory(AVAudioSessionCategoryPlayback, error.ptr)

            audioSession.setMode(AVAudioSessionModeSpokenAudio, error.ptr)

            // 重要：サンプルレートを明示的に設定
            audioSession.setPreferredSampleRate(sampleRate, error.ptr)
            audioSession.setPreferredOutputNumberOfChannels(channelCount.toLong(), error.ptr)

            // バッファサイズも設定（レイテンシ改善）
            audioSession.setPreferredIOBufferDuration(0.023, error.ptr) // ~23ms

            audioSession.setActive(true, error.ptr)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun setupAudioEngine() {
        audioEngine = AVAudioEngine()
        playerNode = AVAudioPlayerNode()

        val engine = audioEngine!!
        val player = playerNode!!

        audioFormat =
            AVAudioFormat(
                commonFormat = AVAudioPCMFormatFloat32,
                sampleRate = sampleRate,
                channels = channelCount,
                interleaved = true,
            )

        Logging.d(
            "🔊 Created format: sampleRate=${audioFormat!!.sampleRate}, channels=${audioFormat!!.channelCount}"
        )

        engine.attachNode(player)
        engine.connect(player, engine.mainMixerNode, audioFormat)

        // 音量調整
        player.volume = 0.8f
        engine.mainMixerNode.outputVolume = 0.9f

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            engine.prepare()
            val success = engine.startAndReturnError(error.ptr)

            if (!success) {
                error.value?.let {
                    Logging.d("🔊 Failed to start engine: ${it.localizedDescription}")
                }
            }
        }

        player.play()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleAudioBuffer(data: ByteArray) {
        val format = audioFormat ?: return
        val player = playerNode ?: return

        if (data.isEmpty()) return

        val frameCount = (data.size / bytesPerSample).toUInt()
        Logging.d("🔊 Processing ${data.size} bytes = $frameCount frames")

        val buffer = AVAudioPCMBuffer(format, frameCount)
        buffer.frameLength = frameCount

        data.usePinned { pinned ->
            val audioBufferList = buffer.audioBufferList?.pointed
            val audioBuffer = audioBufferList?.mBuffers?.pointed

            audioBuffer?.let { buf ->
                val originalSize = buf.mDataByteSize
                Logging.d(
                    "🔊 Buffer: mDataByteSize=${originalSize}, expectedFor16bit=${data.size}, expectedFor32bit=${frameCount * 4u}"
                )

                if (buf.mData != null) {
                    // mDataByteSizeから実際の形式を判定
                    when (originalSize) {
                        frameCount * 4u -> {
                            // 32bit float バッファの場合：16bit → 32bit float変換
                            Logging.d("🔊 Converting 16bit PCM to 32bit float")
                            val floatPtr = buf.mData!!.reinterpret<FloatVar>()

                            for (i in 0 until frameCount.toInt()) {
                                val byteIndex = i * 2
                                if (byteIndex + 1 < data.size) {
                                    val low = data[byteIndex].toInt() and 0xFF
                                    val high = data[byteIndex + 1].toInt() and 0xFF
                                    val sample16bit = (high shl 8) or low

                                    // 16bit signed を float (-1.0 ~ 1.0) に変換
                                    val sampleFloat =
                                        if (sample16bit > 32767) {
                                            (sample16bit - 65536).toFloat() / 32768.0f
                                        } else {
                                            sample16bit.toFloat() / 32767.0f
                                        }

                                    floatPtr[i] = sampleFloat
                                }
                            }

                            // 最初の数値を確認
                            val firstFloats = (0..3).map { floatPtr[it] }
                            Logging.d("🔊 First float samples: $firstFloats")
                        }
                        frameCount * 2u -> {
                            // 16bit integer バッファの場合：そのままコピー
                            Logging.d("🔊 Direct copy for 16bit buffer")
                            memcpy(buf.mData, pinned.addressOf(0), data.size.convert())
                        }
                        else -> {
                            Logging.d("🔊 ERROR: Unexpected buffer size")
                            return@usePinned
                        }
                    }
                } else {
                    Logging.d("🔊 ERROR: Buffer mData is null!")
                    return@usePinned
                }
            }
        }

        player.scheduleBuffer(buffer, completionHandler = null)
        Logging.d("🔊 Buffer scheduled successfully")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cleanup() {
        playerNode?.stop()
        audioEngine?.stop()
        audioEngine?.reset()

        audioEngine = null
        playerNode = null
        audioFormat = null
        playbackQueue.clear()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            AVAudioSession.sharedInstance().setActive(false, error.ptr)
        }
    }

    override fun setCompletedListener(listener: () -> Unit) {
        this.completedListener = listener
    }

    override fun stop() {
        playbackJob?.cancel()
        playbackJob = null
    }
}

actual fun createAudioPlayer(playerScope: CoroutineScope): AudioPlayer = IOSAudioPlayer(playerScope)
