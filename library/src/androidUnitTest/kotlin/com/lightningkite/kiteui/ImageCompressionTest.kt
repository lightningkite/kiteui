// by Claude
package com.lightningkite.kiteui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageCompressionTest {

    private fun ensureContext() {
        try {
            AndroidAppContext.applicationCtx
        } catch (_: UninitializedPropertyAccessException) {
            AndroidAppContext.applicationCtx = RuntimeEnvironment.getApplication()
        }
    }

    /** Creates a colorful test Bitmap and saves it to a temp file, returning a FileReference. */
    private fun createTestImageFile(width: Int, height: Int, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): FileReference {
        ensureContext()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw gradient stripes
        for (y in 0 until height step 10) {
            val r = (255 * y / height)
            val b = 255 - r
            paint.color = android.graphics.Color.rgb(r, 100, b)
            canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + 10).toFloat(), paint)
        }
        // Draw a green circle
        paint.color = android.graphics.Color.rgb(0, 200, 0)
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 4f, paint)

        val ext = if (format == Bitmap.CompressFormat.JPEG) "jpg" else "png"
        val cacheDir = AndroidAppContext.applicationCtx.cacheDir
        val tempFile = File.createTempFile("test-image-", ".$ext", cacheDir)
        tempFile.deleteOnExit()
        tempFile.outputStream().use { out ->
            bitmap.compress(format, 100, out)
        }
        bitmap.recycle()

        return FileReference(Uri.fromFile(tempFile))
    }

    @Test
    fun compressLargeImageFitsWithinBounds() = runTest {
        val ref = createTestImageFile(3000, 2000)
        val originalSize = ref.bytes()

        val result = ImageLocal(ref).compressed(maxWidth = 1024, maxHeight = 1024, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertTrue(resultBytes.isNotEmpty(), "Compressed result should not be empty")
        assertTrue(resultBytes.size < originalSize, "Compressed ${resultBytes.size} should be < original $originalSize")

        // Decode the output and verify dimensions
        val decoded = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
        assertTrue(decoded.width <= 1024, "Width ${decoded.width} should be <= 1024")
        assertTrue(decoded.height <= 1024, "Height ${decoded.height} should be <= 1024")
        // 3000x2000 -> 1024x682
        assertEquals(1024, decoded.width, "Width should be 1024")
        assertEquals(682, decoded.height, "Height should preserve aspect ratio (682)")
        decoded.recycle()

        println("Android large: $originalSize bytes -> ${resultBytes.size} bytes, 1024x682")
    }

    @Test
    fun compressSmallImagePreservesDimensions() = runTest {
        val ref = createTestImageFile(100, 75)

        val result = ImageLocal(ref).compressed(maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertTrue(resultBytes.isNotEmpty())

        val decoded = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
        assertEquals(100, decoded.width, "Width should be unchanged")
        assertEquals(75, decoded.height, "Height should be unchanged")
        decoded.recycle()

        println("Android small: ${ref.bytes()} bytes -> ${resultBytes.size} bytes")
    }

    @Test
    fun compressPortraitImage() = runTest {
        val ref = createTestImageFile(500, 2000)

        val result = ImageLocal(ref).compressed(maxWidth = 512, maxHeight = 512, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        val decoded = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
        assertTrue(decoded.width <= 512)
        assertTrue(decoded.height <= 512)
        // 500x2000 -> 128x512
        assertEquals(128, decoded.width)
        assertEquals(512, decoded.height)
        decoded.recycle()

        println("Android portrait: ${ref.bytes()} bytes -> ${resultBytes.size} bytes, ${decoded.width}x${decoded.height}")
    }

    @Test
    fun qualityAffectsFileSize() = runTest {
        val ref = createTestImageFile(1000, 750)

        val lowQ = ImageLocal(ref).compressed(maxWidth = 1000, maxHeight = 750, quality = 0.1f)
        val highQ = ImageLocal(ref).compressed(maxWidth = 1000, maxHeight = 750, quality = 0.95f)

        val lowBytes = lowQ.data.toByteArray()
        val highBytes = highQ.data.toByteArray()

        assertTrue(lowBytes.size < highBytes.size, "Low quality ${lowBytes.size} should be < high quality ${highBytes.size}")
        println("Android quality: low=${lowBytes.size}, high=${highBytes.size}")
    }

    @Test
    fun compressFromJpegSource() = runTest {
        val ref = createTestImageFile(2000, 1500, Bitmap.CompressFormat.JPEG)
        val originalSize = ref.bytes()

        val result = ImageLocal(ref).compressed(maxWidth = 640, maxHeight = 480, quality = 0.7f)
        val resultBytes = result.data.toByteArray()

        val decoded = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
        assertTrue(decoded.width <= 640)
        assertTrue(decoded.height <= 480)
        decoded.recycle()

        println("Android JPEG->JPEG: $originalSize bytes -> ${resultBytes.size} bytes")
    }
}
