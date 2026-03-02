// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

actual suspend fun ImageLocal.compressed(
    maxWidth: Int,
    maxHeight: Int,
    quality: Float
): ImageRaw {
    // createImageBitmap handles EXIF orientation automatically in modern browsers
    val bitmap = createImageBitmapAwait(file)
    val originalWidth: Int = bitmap.width as Int
    val originalHeight: Int = bitmap.height as Int

    val (targetW, targetH) = calculateScaledSize(originalWidth, originalHeight, maxWidth, maxHeight)

    // Draw onto canvas at target size
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = targetW
    canvas.height = targetH
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    ctx.drawImage(bitmap, 0.0, 0.0, targetW.toDouble(), targetH.toDouble())
    bitmap.close()

    // Convert to JPEG blob
    val blob = canvasToBlobAwait(canvas, "image/jpeg", quality.toDouble())

    return ImageRaw(blob)
}

private suspend fun createImageBitmapAwait(file: org.w3c.files.File): dynamic {
    return suspendCancellableCoroutine<dynamic> { cont ->
        val promise: Promise<dynamic> = js("createImageBitmap(file)") as Promise<dynamic>
        promise.then<dynamic>(
            onFulfilled = { bitmap: dynamic -> cont.resume(bitmap); bitmap },
            onRejected = { error: dynamic -> cont.resumeWithException(Exception("Failed to decode image: $error")); null }
        )
    }
}

private suspend fun canvasToBlobAwait(
    canvas: HTMLCanvasElement,
    type: String,
    quality: Double
): org.w3c.files.Blob {
    return suspendCancellableCoroutine { cont ->
        canvas.asDynamic().toBlob(
            { blob: org.w3c.files.Blob? ->
                if (blob != null) cont.resume(blob)
                else cont.resumeWithException(Exception("canvas.toBlob returned null"))
            },
            type,
            quality
        )
    }
}
