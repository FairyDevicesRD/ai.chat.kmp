package ai.fd.shared.aichat.platform

import kotlinx.coroutines.flow.Flow

/**
 * 音声録音機能を抽象化したインターフェース。
 *
 * このインターフェースは`commonMain`で定義され、 各プラットフォーム（Android/iOS）で具体的な実装が提供される。
 */
interface AudioRecorder {
    class PermissionDeniedException(message: String?) : IllegalStateException(message)

    fun captureFlow(): Flow<ByteArray>
}

expect fun createAudioRecorder(): AudioRecorder
