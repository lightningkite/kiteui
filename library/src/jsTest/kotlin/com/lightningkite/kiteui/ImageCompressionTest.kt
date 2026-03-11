// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import kotlinx.coroutines.test.runTest
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FilePropertyBag
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImageCompressionTest {

    /** Creates a test image as a browser File by drawing to a canvas and exporting as PNG. */
    private suspend fun createTestImageFile(width: Int, height: Int, name: String): File {
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
        val blob = suspendCancellableCoroutine<org.w3c.files.Blob> { cont ->
            canvas.asDynamic().toBlob({ blob: org.w3c.files.Blob? ->
                if (blob != null) cont.resume(blob)
                else cont.resumeWithException(Exception("toBlob returned null"))
            }, "image/png")
        }
        return File(arrayOf(blob), name, FilePropertyBag(type = "image/png"))
    }

    @Test
    fun compressLargeImageFitsWithinBounds() = runTest {
        val file = createTestImageFile(2000, 1500, "large.png")
        val originalSize = file.size.toLong()

        val result = ImageLocal(file).compressed(maxWidth = 800, maxHeight = 800, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertTrue(resultBytes.isNotEmpty(), "Compressed result should not be empty")
        assertTrue(resultBytes.size < originalSize, "Compressed ${resultBytes.size} should be < original $originalSize")

        // Verify the output is a valid image by decoding with createImageBitmap
        val outputFile = File(
            arrayOf(org.w3c.files.Blob(arrayOf(resultBytes.asDynamic()), BlobPropertyBag(type = "image/jpeg"))),
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
        val w: Int = bitmap.width as Int
        val h: Int = bitmap.height as Int
        bitmap.close()

        assertTrue(w <= 800, "Width $w should be <= 800")
        assertTrue(h <= 800, "Height $h should be <= 800")
        println("JS large: $originalSize bytes -> ${resultBytes.size} bytes, ${w}x${h}")
    }

    @Test
    fun compressSmallImagePreservesDimensions() = runTest {
        val file = createTestImageFile(100, 75, "small.png")

        val result = ImageLocal(file).compressed(maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertTrue(resultBytes.isNotEmpty(), "Compressed result should not be empty")

        // Verify dimensions unchanged
        val outputBlob = org.w3c.files.Blob(arrayOf(resultBytes.asDynamic()), BlobPropertyBag(type = "image/jpeg"))
        val bitmap = suspendCancellableCoroutine<dynamic> { cont ->
            val p = js("createImageBitmap(outputBlob)") as Promise<dynamic>
            p.then<dynamic>(
                onFulfilled = { b: dynamic -> cont.resume(b); b },
                onRejected = { e: dynamic -> cont.resumeWithException(Exception("$e")); null }
            )
        }
        val w: Int = bitmap.width as Int
        val h: Int = bitmap.height as Int
        bitmap.close()

        assertTrue(w == 100, "Width should be 100 but was $w")
        assertTrue(h == 75, "Height should be 75 but was $h")
        println("JS small: ${file.size} bytes -> ${resultBytes.size} bytes, ${w}x${h}")
    }

    @Test
    fun qualityAffectsFileSize() = runTest {
        val file = createTestImageFile(800, 600, "quality-test.png")

        val lowQ = ImageLocal(file).compressed(maxWidth = 800, maxHeight = 600, quality = 0.1f)
        val highQ = ImageLocal(file).compressed(maxWidth = 800, maxHeight = 600, quality = 0.95f)

        val lowBytes = lowQ.data.toByteArray()
        val highBytes = highQ.data.toByteArray()

        assertTrue(lowBytes.size < highBytes.size, "Low quality ${lowBytes.size} should be < high quality ${highBytes.size}")
        println("JS quality: low=${lowBytes.size}, high=${highBytes.size}")
    }
}
