package ai.fd.shared.aichat.di

import ai.fd.shared.aichat.domain.usecase.ConversationUseCase
import ai.fd.shared.aichat.platform.AudioPlayer
import ai.fd.shared.aichat.platform.AudioRecorder
import ai.fd.shared.aichat.platform.JpegEditor
import ai.fd.shared.aichat.presentation.viewmodel.AppViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext

@ContributesTo(AppScope::class)
interface ViewModelProviders {

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppViewModel(
        audioRecorder: AudioRecorder,
        audioPlayer: AudioPlayer,
        jpegEditor: JpegEditor,
        conversationUseCase: ConversationUseCase,
        @IoContext recordCoroutineCtx: CoroutineContext,
    ): AppViewModel {
        return AppViewModel(
            audioRecorder = audioRecorder,
            audioPlayer = audioPlayer,
            jpegEditor = jpegEditor,
            conversationUseCase = conversationUseCase,
            recordCoroutineCtx = recordCoroutineCtx,
        )
    }
}
