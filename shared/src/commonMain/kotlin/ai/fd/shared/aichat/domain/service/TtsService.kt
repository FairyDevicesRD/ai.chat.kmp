package ai.fd.shared.aichat.domain.service

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult

interface TtsService {
    suspend fun synthesize(text: String): KResult<ByteArray, DomainError>
}
