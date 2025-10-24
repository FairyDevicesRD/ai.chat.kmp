package ai.fd.shared.aichat.platform

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.getBytes
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
class IosJpegEditor : JpegEditor {
    override fun resizeJpeg(jpegBytes: ByteArray, maxLongSide: Int): ByteArray {
        if (jpegBytes.isEmpty()) {
            throw IllegalArgumentException("jpegBytes cannot be empty")
        }

        if (maxLongSide <= 0) {
            throw IllegalArgumentException("maxLongSide must be positive")
        }

        val data =
            jpegBytes.toCFData()
                ?: throw IllegalStateException("Failed to create CFData from jpeg bytes.")

        // CFDataRefを直接使用
        val imageSource =
            CGImageSourceCreateWithData(data, null)
                ?: throw IllegalStateException("Failed to create image source.")

        // 画像のプロパティを取得
        val properties =
            CGImageSourceCopyPropertiesAtIndex(imageSource, 0u, null)
                ?: throw IllegalStateException("Failed to get image properties.")

        // CFDictionaryから幅と高さを取得
        val width = memScoped {
            val widthRef =
                CFDictionaryGetValue(properties, kCGImagePropertyPixelWidth)
                    ?: throw IllegalStateException("Failed to get image width.")
            val widthValue = alloc<IntVar>()
            CFNumberGetValue(widthRef as CFNumberRef, kCFNumberIntType, widthValue.ptr)
            widthValue.value
        }

        val height = memScoped {
            val heightRef =
                CFDictionaryGetValue(properties, kCGImagePropertyPixelHeight)
                    ?: throw IllegalStateException("Failed to get image height.")
            val heightValue = alloc<IntVar>()
            CFNumberGetValue(heightRef as CFNumberRef, kCFNumberIntType, heightValue.ptr)
            heightValue.value
        }

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("Invalid image dimensions: ${width}x${height}")
        }

        // 画像の向きを判定
        val isPortrait = height > width
        val longSide = if (isPortrait) height else width
        val shortSide = if (isPortrait) width else height

        // リサイズが不要な場合は元の画像を返す
        if (longSide <= maxLongSide) {
            return jpegBytes
        }

        // 新しいサイズを計算
        val ratio = maxLongSide.toDouble() / longSide
        val newShortSide = (shortSide * ratio).toInt()
        val targetWidth = if (isPortrait) newShortSide else maxLongSide
        val targetHeight = if (isPortrait) maxLongSide else newShortSide

        // ダウンサンプリングオプションを設定（CFDictionaryを直接作成）
        val options = CFDictionaryCreateMutable(kCFAllocatorDefault, 2, null, null)

        // kCGImageSourceCreateThumbnailFromImageAlways = true
        CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)

        // kCGImageSourceThumbnailMaxPixelSize = maxLongSide
        val maxSizeNumber = memScoped {
            val value = alloc<IntVar>()
            value.value = maxLongSide
            CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, value.ptr)
        }
        CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, maxSizeNumber)

        // ダウンサンプリングされた画像を作成
        val downsampledCGImage =
            CGImageSourceCreateThumbnailAtIndex(imageSource, 0u, options)
                ?: throw IllegalStateException("Failed to create downsampled image.")

        val downsampledUIImage = UIImage(cGImage = downsampledCGImage)

        // 最終的なターゲットサイズにリサイズ
        val targetSize = CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble())
        val renderer = UIGraphicsImageRenderer(size = targetSize)

        val resizedUIImage =
            renderer.imageWithActions { _: UIGraphicsImageRendererContext? ->
                downsampledUIImage.drawInRect(
                    CGRectMake(0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble())
                )
            }

        // JPEG形式でエンコード（品質0.9）
        val resizedData =
            UIImageJPEGRepresentation(resizedUIImage, 0.9)
                ?: throw IllegalStateException("Failed to encode JPEG data.")

        return resizedData.toByteArray()
    }

    override fun toJpegByteArray(imageBitmap: ImageBitmap): ByteArray? {
        val uiImage = imageBitmap.toUIImage() ?: return null
        return UIImageJPEGRepresentation(uiImage, 1.0)?.toByteArray()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? {
    if (isEmpty()) return null

    return usePinned {
        // CFDataを直接作成（Swiftの __bridge CFDataRef に相当）
        CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), size.toLong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val dataLength = length.toInt()
    if (dataLength == 0) return ByteArray(0)

    return ByteArray(dataLength).apply { usePinned { getBytes(it.addressOf(0), length) } }
}

@OptIn(ExperimentalForeignApi::class)
fun ImageBitmap.toUIImage(): UIImage? {
    val width = this.width
    val height = this.height

    if (width <= 0 || height <= 0) return null

    val buffer = IntArray(width * height)

    try {
        this.readPixels(buffer)
    } catch (e: Exception) {
        return null
    }

    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null

    val context =
        buffer.usePinned { pinnedBuffer ->
            CGBitmapContextCreate(
                data = pinnedBuffer.addressOf(0),
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow = (4 * width).toULong(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
        } ?: return null

    val cgImage = CGBitmapContextCreateImage(context) ?: return null

    return UIImage.imageWithCGImage(cgImage)
}

actual fun createJpegEditor(): JpegEditor {
    return IosJpegEditor()
}
