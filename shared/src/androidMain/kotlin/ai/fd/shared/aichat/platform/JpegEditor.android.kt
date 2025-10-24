package ai.fd.shared.aichat.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

class AndroidJpegEditor : JpegEditor {
    override fun resizeJpeg(jpegBytes: ByteArray, maxLongSide: Int): ByteArray {
        // 1. 元画像のサイズを取得（デコード境界のみ）
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true // デコードせずにサイズ情報のみを取得
            }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

        val srcWidth = options.outWidth
        val srcHeight = options.outHeight

        // 2. 新しいリサイズ目標サイズを計算（共通ロジック）
        val isPortrait = srcHeight > srcWidth
        val srcLongSide = if (isPortrait) srcHeight else srcWidth
        val srcShortSide = if (isPortrait) srcWidth else srcHeight

        // リサイズしない場合
        if (srcLongSide <= maxLongSide) {
            return jpegBytes
        }

        val ratio = maxLongSide.toDouble() / srcLongSide
        val newShortSide = (srcShortSide * ratio).toInt()

        val targetWidth = if (isPortrait) newShortSide else maxLongSide
        val targetHeight = if (isPortrait) maxLongSide else newShortSide

        // 3. サンプリングサイズの設定 (inSampleSize)
        // デコード時のメモリ消費を抑えるため、最適な縮小率を求める
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(srcWidth, srcHeight, targetWidth, targetHeight)

        // 4. ダウンサンプリングされたBitmapの取得
        val downsampledBitmap =
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)
                ?: throw IllegalStateException("Failed to decode JPEG data.")

        // 5. 最終目標サイズに正確にリサイズ
        // inSampleSizeだけでは目標サイズに正確にならないため、createScaledBitmapで最終調整
        val resizedBitmap = downsampledBitmap.scale(targetWidth, targetHeight)

        // ダウンサンプリングされた元BitmapがresizedBitmapと異なる場合は解放（メモリリーク防止）
        if (downsampledBitmap != resizedBitmap) {
            downsampledBitmap.recycle()
        }

        // 6. リサイズ後のBitmapをJPEGバイト配列にエンコード
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // 90は画質（0-100）

        resizedBitmap.recycle() // 処理完了後のBitmap解放

        return outputStream.toByteArray()
    }

    override fun toJpegByteArray(imageBitmap: ImageBitmap): ByteArray? {
        val androidBitmap = imageBitmap.asAndroidBitmap()

        return try {
            ByteArrayOutputStream().use { outputStream ->
                androidBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Bitmapをデコードする際の inSampleSize を計算するヘルパー関数 メモリ使用量を削減するために、デコード前に画像を大まかに縮小するための値。 */
    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Int {
        var inSampleSize = 1

        // 目標サイズに対して幅と高さが2倍以上になるまでinSampleSizeを増やす
        if (srcHeight > targetHeight || srcWidth > targetWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while (
                (halfHeight / inSampleSize) >= targetHeight &&
                    (halfWidth / inSampleSize) >= targetWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

actual fun createJpegEditor(): JpegEditor = AndroidJpegEditor()
