package ai.fd.shared.aichat.domain.service.impl.engine

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ChatRequest(val contents: List<Content>, val systemInstruction: Content)

@Serializable data class Content(val role: String, val parts: List<Part>)

@Serializable
data class Part(val text: String? = null, val inlineData: InlineData? = null) {
    init {
        require(text != null || inlineData != null)
    }
}

@Serializable
data class InlineData(@SerialName("mime_type") val mimeType: String, val data: String)

@Serializable data class ChatResponse(val candidates: List<Candidate>)

@Serializable
data class Candidate(
    val content: Content,
    @SerialName("finishReason") val finishReason: String? = null,
)

interface GeminiApiService {
    @POST("v1beta/models/{modelName}:generateContent")
    suspend fun generateContent(
        @Path("modelName") modelName: String,
        @Query("key") apiKey: String,
        @Body request: ChatRequest,
    ): ChatResponse
}
