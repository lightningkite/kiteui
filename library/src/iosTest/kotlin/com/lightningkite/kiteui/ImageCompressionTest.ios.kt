package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Size
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ImageCompressionTestIOS : ImageCompressionTest() {

    override suspend fun createTestImage(width: Int, height: Int, name: String): FileReference {
        val size = CGSizeMake(width.toDouble(), height.toDouble())
        UIGraphicsBeginImageContextWithOptions(size, true, 1.0)
        val ctx = UIGraphicsGetCurrentContext()!!

        // Draw gradient stripes
        for (y in 0 until height step 10) {
            val r = y.toDouble() / height
            val b = 1.0 - r
            CGContextSetRGBFillColor(ctx, r, 0.4, b, 1.0)
            CGContextFillRect(ctx, CGRectMake(0.0, y.toDouble(), width.toDouble(), 10.0))
        }
        // Draw a green circle
        CGContextSetRGBFillColor(ctx, 0.0, 0.8, 0.0, 1.0)
        CGContextFillEllipseInRect(
            ctx, CGRectMake(
                width / 4.0, height / 4.0,
                width / 2.0, height / 2.0
            )
        )

        val image = UIGraphicsGetImageFromCurrentImageContext()!!
        UIGraphicsEndImageContext()

        val ext = name.substringAfterLast('.')
        val data = when (ext) {
            "png" -> UIImagePNGRepresentation(image)
            else -> UIImageJPEGRepresentation(image, 1.0)
        }
            ?: throw IllegalStateException("Failed to create Image data")

        val utType =
            when (ext) {
                "png" -> UTTypePNG
                else -> UTTypeJPEG
            }
        val provider = platform.Foundation.NSItemProvider(item = data, typeIdentifier = utType.identifier)
        provider.suggestedName = name
        return FileReference(provider, utType)
    }

    override fun createNonImageFile(): FileReference {
        val data = "Hello World!".nsdata()
            ?: throw IllegalStateException("Failed to create Text data")
        val provider = platform.Foundation.NSItemProvider(item = data, typeIdentifier = UTTypePlainText.identifier)
        provider.suggestedName = "not-an-image.txt"
        return FileReference(provider, UTTypePlainText)
    }

    override suspend fun ByteArray.getImageSize(): Size {
        val decoded = UIImage(data = this.toNSData())
        return Size(decoded.size.useContents { width }, decoded.size.useContents { height })
    }

    override suspend fun FileReference.getRawBytes(): ByteArray {
        return suspendCoroutine { cont ->
            this.provider.loadDataRepresentationForContentType(
                this.suggestedType ?: UTTypePlainText
            ) { data, error ->
                if (data != null) {
                    cont.resume(data.toByteArray())
                } else {
                    cont.resumeWithException(Exception(error?.description))
                }
            }
        }
    }

    override suspend fun FileReference.getSize(): Long {
        return suspendCoroutine { cont ->
            this.provider.loadDataRepresentationForContentType(
                this.suggestedType ?: UTTypePlainText
            ) { data, error ->
                if (data != null) {
                    cont.resume(data.length.toLong())
                } else {
                    cont.resumeWithException(Exception(error?.description))
                }
            }
        }
    }
}
