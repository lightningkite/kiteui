package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Size
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

class ImageCompressionTestJS : ImageCompressionTest() {

    override suspend fun createTestImage(width: Int, height: Int, name: String): FileReference {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = width
        canvas.height = height
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

        // Draw a colorful gradient so compression differences are visible
        for (y in 0 until height step 10) {
            val r = (255 * y / height)
            val b = 255 - r
            ctx.fillStyle = "rgb($r, 100, $b)"
            ctx.fillRect(0.0, y.toDouble(), width.toDouble(), 10.0)
        }
        // Draw a green circle in the center
        ctx.fillStyle = "rgb(0, 200, 0)"
        ctx.beginPath()
        ctx.arc(width / 2.0, height / 2.0, minOf(width, height) / 4.0, 0.0, 6.28)
        ctx.fill()

        // Export canvas as PNG blob, then wrap as File
        val type = when(val ext = name.substringAfterLast('.')){
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        val blob = suspendCancellableCoroutine<org.w3c.files.Blob> { cont ->
            canvas.asDynamic().toBlob(
                { blob: org.w3c.files.Blob? ->
                    if (blob != null) cont.resume(blob)
                    else cont.resumeWithException(Exception("toBlob returned null"))
                },
                type
            )
        }
        return File(arrayOf(blob), name, FilePropertyBag(type = type))
    }

    override fun createNonImageFile(): FileReference =
        File(arrayOf("Hello World!".toBlob()), "not-an-image.txt", FilePropertyBag(type = "text/plain"))


    override suspend fun ByteArray.getImageSize(): Size {

        val outputFile = File(
            arrayOf(org.w3c.files.Blob(arrayOf(this.asDynamic()), BlobPropertyBag(type = "image/jpeg"))),
            "output.jpg",
            FilePropertyBag(type = "image/jpeg")
        )

        val bitmap = suspendCancellableCoroutine<dynamic> { cont ->
            val p = js("createImageBitmap(outputFile)") as Promise<dynamic>
            p.then<dynamic>(
                onFulfilled = { b: dynamic -> cont.resume(b); b },
                onRejected = { e: dynamic -> cont.resumeWithException(Exception("$e")); null }
            )
        }
        val w = bitmap.width
        val h = bitmap.height
        bitmap.close()
        return Size(w, h)
    }

    override suspend fun FileReference.getRawBytes(): ByteArray = this.toByteArray()
    override suspend fun FileReference.getSize(): Long = this.bytes()

}
