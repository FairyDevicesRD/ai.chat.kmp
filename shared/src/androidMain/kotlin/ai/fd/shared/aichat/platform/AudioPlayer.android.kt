package ai.fd.shared.aichat.platform

import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidAudioPlayer(private val playerScope: CoroutineScope) : AudioPlayer {

    // サンプルレートは固定値として定義
    private val sampleRate = 16000

    private var audioTrack: AudioTrack? = null
    private val playbackQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackJob: Job? = null

    private var completedListener: (() -> Unit)? = null

    override fun play(pcmData: ByteArray) {
        // 再生キューに新しいデータを追加
        playbackQueue.add(pcmData)

        // 再生ジョブが動いていなければ開始
        if (playbackJob?.isActive != true) {
            startPlayback()
        }
    }

    private fun startPlayback() {
        // 既存の再生ジョブをキャンセル
        playbackJob?.cancel()

        // 新しいコルーチンを開始
        playbackJob =
            playerScope.launch {
                try {
                    val bufferSize =
                        AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                        )

                    audioTrack =
                        AudioTrack.Builder()
                            .setAudioAttributes(
                                android.media.AudioAttributes.Builder()
                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                    .setContentType(
                                        android.media.AudioAttributes.CONTENT_TYPE_SPEECH
                                    )
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()

                    audioTrack?.play()

                    while (playbackQueue.isNotEmpty() && isActive) {
                        val data = playbackQueue.poll() ?: return@launch
                        audioTrack?.write(data, 0, data.size)
                    }

                    // すべての再生が完了したらリスナーを呼び出す
                    completedListener?.invoke()
                } finally {
                    withContext(NonCancellable) {
                        audioTrack?.stop()
                        audioTrack?.release()
                        audioTrack = null
                        playbackQueue.clear()
                    }
                }
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

actual fun createAudioPlayer(playerScope: CoroutineScope): AudioPlayer =
    AndroidAudioPlayer(playerScope)
