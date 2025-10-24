package ai.fd.shared.aichat.platform

import androidx.compose.ui.graphics.ImageBitmap

interface JpegEditor {
    /**
     * JPEG画像のバイト配列をリサイズする。 元画像の縦横比を維持し、長辺が指定されたサイズになるように調整する。
     * * @param jpegBytes 元画像のJPEGバイト配列
     *
     * @param maxLongSide 目標とする長辺の最大サイズ（ピクセル）
     * @return リサイズ後のJPEGバイト配列
     */
    fun resizeJpeg(jpegBytes: ByteArray, maxLongSide: Int): ByteArray

    /** ImageBitmapをJPEGに変換する */
    fun toJpegByteArray(imageBitmap: ImageBitmap): ByteArray?
}

expect fun createJpegEditor(): JpegEditor
