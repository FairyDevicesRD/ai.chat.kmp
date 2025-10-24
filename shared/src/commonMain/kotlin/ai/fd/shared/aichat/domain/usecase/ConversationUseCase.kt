package ai.fd.shared.aichat.domain.usecase

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult

interface ConversationUseCase {
    suspend operator fun invoke(
        pcmData: ByteArray,
        jpeg: ByteArray?,
        callback: ConversationCallback,
    ): KResult<Unit, DomainError>

    interface ConversationCallback {
        fun onAsrRecognized(text: String)

        fun onAiAnswer(text: String)

        fun onTtsSynthesized(pcm: ByteArray)
    }
}
