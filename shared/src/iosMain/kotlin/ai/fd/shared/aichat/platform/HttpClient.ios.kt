package ai.fd.shared.aichat.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createMimiHttpClient(): HttpClient {
    return HttpClient(Darwin)
}

actual fun createAiHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(DefaultRequest) { header(HttpHeaders.ContentType, ContentType.Application.Json) }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }
            )
        }
    }
}
