package ai.fd.shared.aichat.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
private class AudioRecorderAndroid() : AudioRecorder {
    override fun captureFlow(): Flow<ByteArray> = callbackFlow {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val buffer = ByteArray(bufferSize)

        val audioRecord =
            try {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } catch (e: SecurityException) {
                throw AudioRecorder.PermissionDeniedException(e.message)
            }
        audioRecord.startRecording()
        val recordingJob = launch {
            while (isActive && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    trySend(buffer.copyOf(read))
                }
            }
        }

        awaitClose {
            recordingJob.cancel()
            audioRecord.stop()
            audioRecord.release()
        }
    }
}

actual fun createAudioRecorder(): AudioRecorder = AudioRecorderAndroid()
