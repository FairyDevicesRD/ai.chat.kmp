package ai.fd.shared.aichat.di

import ai.fd.mimi.client.engine.MimiNetworkEngine
import ai.fd.mimi.client.engine.ktor.Ktor
import ai.fd.mimi.client.service.token.MimiTokenService
import ai.fd.shared.aichat.BuildKonfig
import ai.fd.shared.aichat.domain.service.TokenService
import ai.fd.shared.aichat.domain.service.impl.GeminiConfig
import ai.fd.shared.aichat.platform.AudioPlayer
import ai.fd.shared.aichat.platform.AudioRecorder
import ai.fd.shared.aichat.platform.JpegEditor
import ai.fd.shared.aichat.platform.createAiHttpClient
import ai.fd.shared.aichat.platform.createAudioPlayer
import ai.fd.shared.aichat.platform.createAudioRecorder
import ai.fd.shared.aichat.platform.createJpegEditor
import ai.fd.shared.aichat.platform.createMimiHttpClient
import ai.fd.shared.aichat.platform.ioCoroutineContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope

@ContributesTo(AppScope::class)
@BindingContainer
object PlatformBindings {
    @Provides fun provideAudioRecorder(): AudioRecorder = createAudioRecorder()

    @Provides @MimiHttp fun provideMimiHttpClient(): HttpClient = createMimiHttpClient()

    @Provides @AiHttp fun provideAiHttpClient(): HttpClient = createAiHttpClient()

    @Provides
    fun provideAudioPlayer(@IoScope ioCoroutineScope: CoroutineScope): AudioPlayer =
        createAudioPlayer(ioCoroutineScope)

    @Provides fun provideJpegEditor(): JpegEditor = createJpegEditor()

    @Provides @IoContext fun provideIoCoroutineContext(): CoroutineContext = ioCoroutineContext()
}

@ContributesTo(AppScope::class)
@BindingContainer
object MimiBindings {
    @Provides
    fun provideMimiNetworkEngineFactory(
        @MimiHttp mimiHttpClient: HttpClient
    ): MimiNetworkEngine.Factory = MimiNetworkEngine.Ktor(mimiHttpClient)

    @Provides
    fun provideMimiTokenService(factory: MimiNetworkEngine.Factory): MimiTokenService =
        MimiTokenService(factory)

    @Provides
    fun provideCredentials(): TokenService.Credentials =
        TokenService.Credentials(
            applicationId = BuildKonfig.mimiApplicationId,
            clientId = BuildKonfig.mimiClientId,
            clientSecret = BuildKonfig.mimiClientSecret,
        )
}

@ContributesTo(AppScope::class)
@BindingContainer
object AiBindings {
    @Provides
    fun provideGeminiConfig(): GeminiConfig =
        GeminiConfig(
            apiKey = BuildKonfig.geminiApiKey,
            endpoint = BuildKonfig.geminiEndpoint,
            model = BuildKonfig.geminiModel,
        )
}

@ContributesTo(AppScope::class)
@BindingContainer
object CoreBindings {
    @Provides @OptIn(ExperimentalTime::class) fun provideClock(): Clock = Clock.System
}

@ContributesTo(AppScope::class)
@BindingContainer
object CoroutineBindings {
    @Provides
    @IoScope
    fun provideIoCoroutineScope(@IoContext ioCoroutineContext: CoroutineContext): CoroutineScope =
        CoroutineScope(ioCoroutineContext)
}
