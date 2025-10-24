package ai.fd.shared.aichat.domain.service.impl

import ai.fd.mimi.client.engine.MimiNetworkEngine
import ai.fd.mimi.client.service.nict.asr.MimiNictAsrV2Service
import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult
import ai.fd.shared.aichat.domain.service.AsrService
import ai.fd.shared.aichat.domain.service.TokenService
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.utils.io.CancellationException

@Inject
@ContributesBinding(AppScope::class)
class AsrServiceImpl(
    private val engineFactory: MimiNetworkEngine.Factory,
    private val tokenService: TokenService,
) : AsrService {
    override suspend fun recognize(audioData: ByteArray): KResult<String, DomainError> {
        val token = tokenService.getOrCreateToken()
        if (token.isErr) return Err(token.unwrapError())

        val asr = MimiNictAsrV2Service(engineFactory = engineFactory, accessToken = token.unwrap())
        val result = asr.requestAsr(audioData)
        return try {
            Ok(result.getOrThrow().response[0].result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Err(DomainError.AsrError(e))
        }
    }
}
