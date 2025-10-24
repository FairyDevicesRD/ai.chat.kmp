package ai.fd.shared.aichat.domain.service.impl

import ai.fd.mimi.client.service.token.MimiIssueTokenResult
import ai.fd.mimi.client.service.token.MimiTokenService
import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult
import ai.fd.shared.aichat.Logging
import ai.fd.shared.aichat.domain.service.TimeService
import ai.fd.shared.aichat.domain.service.TokenService
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.utils.io.CancellationException

@Inject
@ContributesBinding(AppScope::class)
class TokenServiceImpl(
    private val mimiTokenService: MimiTokenService,
    private val timeService: TimeService,
    private val credentials: TokenService.Credentials,
) : TokenService {
    private var tokenCache: MimiIssueTokenResult? = null

    override suspend fun getOrCreateToken(): KResult<String, DomainError> {
        return try {
            val token =
                tokenCache.getTokenByCache(timeService)
                    ?: run {
                        val result =
                            mimiTokenService.issueClientAccessToken(
                                applicationId = credentials.applicationId,
                                clientId = credentials.clientId,
                                clientSecret = credentials.clientSecret,
                                scopes = credentials.scopes,
                            )
                        tokenCache = result.getOrThrow()
                        tokenCache!!.accessToken
                    }
            Ok(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Err(DomainError.GetTokenError(e))
        }
    }
}

private fun MimiIssueTokenResult?.getTokenByCache(timeService: TimeService): String? {
    return if (
        this != null && (this.startTimestamp + this.expiresIn) > timeService.epochSeconds()
    ) {
        Logging.d("token cache hit")
        this.accessToken
    } else null
}
