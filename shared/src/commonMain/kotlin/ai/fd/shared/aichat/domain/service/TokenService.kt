package ai.fd.shared.aichat.domain.service

import ai.fd.mimi.client.service.token.MimiTokenScope
import ai.fd.mimi.client.service.token.MimiTokenScopes
import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult

interface TokenService {
    suspend fun getOrCreateToken(): KResult<String, DomainError>

    data class Credentials(
        val applicationId: String,
        val clientId: String,
        val clientSecret: String,
        val scopes: Set<MimiTokenScope> =
            setOf(MimiTokenScopes.NictAsr.Api.Http, MimiTokenScopes.Tts.Api.Http),
    )
}
