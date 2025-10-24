package ai.fd.shared.aichat.domain.service.impl

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult
import ai.fd.shared.aichat.di.AiHttp
import ai.fd.shared.aichat.domain.service.AiAgentService
import ai.fd.shared.aichat.domain.service.impl.engine.ChatRequest
import ai.fd.shared.aichat.domain.service.impl.engine.Content
import ai.fd.shared.aichat.domain.service.impl.engine.GeminiApiService
import ai.fd.shared.aichat.domain.service.impl.engine.InlineData
import ai.fd.shared.aichat.domain.service.impl.engine.Part
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import de.jensklingenberg.ktorfit.Ktorfit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.utils.io.CancellationException
import kotlin.io.encoding.Base64

data class GeminiConfig(val apiKey: String, val endpoint: String, val model: String)

@Inject
@ContributesBinding(AppScope::class)
class AiAgentServiceImpl(
    @AiHttp private val httpClient: HttpClient,
    private val config: GeminiConfig,
) : AiAgentService {
    private val apiService: GeminiApiService by
        lazy(LazyThreadSafetyMode.NONE) {
            val ktorfit = Ktorfit.Builder().httpClient(httpClient).baseUrl(config.endpoint).build()
            ktorfit.create()
        }

    override suspend fun question(
        text: String,
        jpeg: ByteArray?,
    ): KResult<List<String>, DomainError> {
        return try {
            val parts = mutableListOf<Part>()
            parts.add(Part(text = text))
            if (jpeg != null) {
                parts.add(
                    Part(
                        inlineData = InlineData(mimeType = "image/jpeg", data = Base64.encode(jpeg))
                    )
                )
            }
            val request =
                ChatRequest(
                    contents = listOf(Content(role = "user", parts = parts.toList())),
                    systemInstruction =
                        Content(role = "model", parts = listOf(Part(text = "返答は日本語"))),
                )

            val response =
                apiService.generateContent(
                    modelName = config.model,
                    apiKey = config.apiKey,
                    request = request,
                )

            Ok(
                response.candidates
                    .mapNotNull { it.content.parts.firstOrNull()?.text }
                    .flatMap { it.split("\n") }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Err(DomainError.AiAgentError(e))
        }
    }
}
