// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import kotlin.coroutines.*

actual suspend fun ImageLocal.compressed(
    maxWidth: Int,
    maxHeight: Int,
    quality: Float,
): ImageRaw {

    // Load raw data from NSItemProvider
    val type = file.suggestedType
        ?: (file.provider.registeredContentTypes.firstOrNull() as? UTType)
        ?: UTTypeImage

    val imageData: NSData = suspendCoroutine { continuation ->
        file.provider.loadDataRepresentationForContentType(type) { data, error ->
            if (error != null) {
                if (type != UTTypeData) {
                    file.provider.loadDataRepresentationForContentType(UTTypeData) { data2, error2 ->
                        if (error2 != null) {
                            continuation.resumeWithException(Exception(error2.description))
                        } else {
                            continuation.resume(data2 ?: throw Exception("Image data is null"))
                        }
                    }
                } else {
                    continuation.resumeWithException(Exception(error.description))
                }
            } else {
                continuation.resume(data ?: throw Exception("Image data is null"))
            }
        }
    }

    // UIImage automatically handles EXIF orientation
    val originalImage = UIImage(data = imageData)

    val originalWidth = originalImage.size.useContents { width }.toInt()
    val originalHeight = originalImage.size.useContents { height }.toInt()

    // Short circuit, no need to compress because the size and type already match expectations
    if (maxWidth >= originalWidth && maxHeight >= originalHeight && type == UTTypeJPEG)
        return ImageRaw(imageData.toByteArray().toBlob("image/jpeg"))

    val (targetW, targetH) = calculateScaledSize(originalWidth, originalHeight, maxWidth, maxHeight)

    // Resize using Core Graphics
    val resizedImage = if (targetW == originalWidth && targetH == originalHeight) {
        originalImage
    } else {
        val targetSize = CGSizeMake(targetW.toDouble(), targetH.toDouble())
        UIGraphicsBeginImageContextWithOptions(targetSize, true, 1.0)
        originalImage.drawInRect(CGRectMake(0.0, 0.0, targetW.toDouble(), targetH.toDouble()))
        val result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        result ?: throw IllegalStateException("Failed to resize image")
    }

    // Compress to JPEG
    val jpegData = UIImageJPEGRepresentation(resizedImage, quality.toDouble())
        ?: throw IllegalStateException("Failed to compress image to JPEG")

    return ImageRaw(Blob(jpegData, "image/jpeg"))
}
