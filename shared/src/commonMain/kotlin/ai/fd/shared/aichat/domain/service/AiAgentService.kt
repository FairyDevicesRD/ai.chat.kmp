package ai.fd.shared.aichat.domain.service

import ai.fd.shared.aichat.DomainError
import ai.fd.shared.aichat.KResult

interface AiAgentService {
    /**
     * 質問をAIに投げる
     *
     * @param text 質問
     * @param jpeg カメラ画像(nullのときは省略)
     * @return AIの回答(改行区切りでList)
     */
    suspend fun question(text: String, jpeg: ByteArray? = null): KResult<List<String>, DomainError>
}
