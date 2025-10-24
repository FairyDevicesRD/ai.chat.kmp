package ai.fd.shared.aichat.domain.service

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult

interface AsrService {
    suspend fun recognize(audioData: ByteArray): KResult<String, DomainError>
}
