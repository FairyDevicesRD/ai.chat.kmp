package ai.fd.shared.aichat.domain.service.impl

import ai.fd.mimi.client.engine.MimiNetworkEngine
import ai.fd.mimi.client.service.nict.tts.MimiNictTtsOptions
import ai.fd.mimi.client.service.nict.tts.MimiNictTtsService
import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult
import ai.fd.shared.aichat.domain.service.TokenService
import ai.fd.shared.aichat.domain.service.TtsService
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
class TtsServiceImpl(
    private val engineFactory: MimiNetworkEngine.Factory,
    private val tokenService: TokenService,
) : TtsService {
    override suspend fun synthesize(text: String): KResult<ByteArray, DomainError> {
        val token = tokenService.getOrCreateToken()
        if (token.isErr) return Err(token.unwrapError())

        val tts = MimiNictTtsService(engineFactory = engineFactory, accessToken = token.unwrap())

        return try {
            val result =
                tts.requestTts(
                    text = text,
                    options =
                        MimiNictTtsOptions(
                            language = MimiNictTtsOptions.Language.JA,
                            audioFormat = MimiNictTtsOptions.AudioFormat.RAW,
                            audioEndian = MimiNictTtsOptions.AudioEndian.LITTLE,
                            gender = MimiNictTtsOptions.Gender.FEMALE,
                            rate = 1.0f, // 0.5 ~ 2.0
                        ),
                )
            Ok(result.getOrThrow().audioBinary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Err(DomainError.TtsError(e))
        }
    }
}
