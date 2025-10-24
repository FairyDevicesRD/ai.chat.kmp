package ai.fd.shared.aichat.platform

import kotlinx.coroutines.CoroutineScope

interface AudioPlayer {
    /**
     * PCMデータを再生する. すでに再生中の場合は、次に再生する。
     *
     * @param pcmData 再生するPCMデータ
     */
    fun play(pcmData: ByteArray)

    /** 再生を停止する */
    fun stop()

    /** すべての音声データの再生が完了したときに呼び出されるリスナーを設定する */
    fun setCompletedListener(listener: () -> Unit)
}

expect fun createAudioPlayer(playerScope: CoroutineScope): AudioPlayer
