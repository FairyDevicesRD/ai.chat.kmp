package ai.fd.shared.aichat

import com.github.michaelbull.result.Result

typealias KResult<D, E> = Result<D, E>

// e.g.
// fun a(): KResult<Int, String> {
//    return Ok(1)
// }

sealed interface Error

private typealias RootError = Error

sealed interface DomainError : RootError {

    sealed interface OperationError : DomainError {
        // Permissionエラー
        data object PermissionDenied : OperationError

        // 音声再生エラー
        class PlayerError(val cause: Throwable?) : OperationError

        // 録音エラー
        class MicError(val cause: Throwable?) : OperationError
    }

    class GetTokenError(val cause: Throwable?) : DomainError

    class AsrError(val cause: Throwable?) : DomainError

    class TtsError(val cause: Throwable?) : DomainError

    object EmptyAsrError : DomainError

    class AiAgentError(val cause: Throwable?) : DomainError
}
