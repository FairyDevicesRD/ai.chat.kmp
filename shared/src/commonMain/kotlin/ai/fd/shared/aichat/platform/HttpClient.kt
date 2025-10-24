package ai.fd.shared.aichat.platform

import io.ktor.client.HttpClient

expect fun createMimiHttpClient(): HttpClient

expect fun createAiHttpClient(): HttpClient
