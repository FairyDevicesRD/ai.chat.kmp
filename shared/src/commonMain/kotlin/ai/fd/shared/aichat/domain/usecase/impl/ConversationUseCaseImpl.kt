package ai.fd.shared.aichat.domain.usecase.impl

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult
import ai.fd.shared.aichat.Logging
import ai.fd.shared.aichat.domain.service.AiAgentService
import ai.fd.shared.aichat.domain.service.AsrService
import ai.fd.shared.aichat.domain.service.TtsService
import ai.fd.shared.aichat.domain.usecase.ConversationUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class ConversationUseCaseImpl(
    private val asrService: AsrService,
    private val aiAgentService: AiAgentService,
    private val ttsService: TtsService,
) : ConversationUseCase {
    /**
     * 録音されたPCMデータを処理し、AIの回答を音声データとして返す
     *
     * @param pcmData ユーザーの質問音声（PCMデータ）
     * @param jpeg 画像データ
     * @return AIの回答音声（PCMデータ）
     */
    override suspend operator fun invoke(
        pcmData: ByteArray,
        jpeg: ByteArray?,
        callback: ConversationUseCase.ConversationCallback,
    ): KResult<Unit, DomainError> {
        // 1. ASR: PCMデータをテキストに変換
        val userQuestionTextResult = asrService.recognize(pcmData)
        if (userQuestionTextResult.isErr) return Err(userQuestionTextResult.unwrapError())
        // 2. ASR: empty -> return ERROR
        val userQuestionText =
            userQuestionTextResult.unwrap().takeIf { it.isNotEmpty() }
                ?: return Err(DomainError.EmptyAsrError)
        Logging.d("userQuestionText: $userQuestionText")
        callback.onAsrRecognized(userQuestionText)

        // 3. AI Question: テキストをAIに投げて回答を得る
        val aiResponseListResult = aiAgentService.question(text = userQuestionText, jpeg = jpeg)
        if (aiResponseListResult.isErr) return Err(aiResponseListResult.unwrapError())
        val aiResponseList = aiResponseListResult.unwrap()
        Logging.d("aiResponseText: ${aiResponseList.joinToString(separator = "\n")}")
        callback.onAiAnswer(aiResponseList.joinToString(separator = "\n"))

        // 4. TTS: AIの回答テキストを音声データに変換
        aiResponseList
            .map { it.trim() }
            .filter { it.isNotBlank() }
            // マークダウン除去を追加
            .map { it.removeMarkdownFormatting() }
            .filter { it.isNotBlank() }
            .forEach {
                val pcm = ttsService.synthesize(it)
                if (pcm.isOk) callback.onTtsSynthesized(pcm.unwrap())
            }
        return Ok(Unit)
    }

    // マークダウン記法を除去する拡張関数
    // TODO: unitTest
    private fun String.removeMarkdownFormatting(): String {
        return this
            // **太字** と *斜体* を除去
            .replace(Regex("""\*\*([^*]+)\*\*"""), "$1") // **text** → text
            .replace(Regex("""\*([^*]+)\*"""), "$1") // *text* → text

            // その他のマークダウン記法も除去
            .replace(Regex("""__([^_]+)__"""), "$1") // __text__ → text
            .replace(Regex("""_([^_]+)_"""), "$1") // _text_ → text
            .replace(Regex("""`([^`]+)`"""), "$1") // `code` → code
            .replace(Regex("""~~([^~]+)~~"""), "$1") // ~~strikethrough~~ → strikethrough

            // リンク記法を除去 [text](url) → text
            .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1")

            // ヘッダー記法を除去 ### text → text
            .replace(Regex("""^#+\s*"""), "")

            // リスト記法の先頭記号を除去
            .replace(Regex("""^[\s]*[-*+]\s+"""), "") // - item → item
            .replace(Regex("""^[\s]*\d+\.\s+"""), "") // 1. item → item
            .trim()
    }
}
