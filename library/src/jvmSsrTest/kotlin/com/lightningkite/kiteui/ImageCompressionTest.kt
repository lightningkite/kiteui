// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ImageCompressionTest {

    init {
        // by Claude - AWT hangs without headless mode when system graphics services are unavailable
        System.setProperty("java.awt.headless", "true")
    }

    private val outputDir = File("build/test-output/image-compression").apply { mkdirs() }

    /** Creates a colorful test image with gradients so visual differences are obvious. */
    private fun createTestImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.paint = GradientPaint(0f, 0f, Color.RED, width.toFloat(), height.toFloat(), Color.BLUE)
        g.fillRect(0, 0, width, height)
        g.paint = GradientPaint(0f, height.toFloat(), Color.GREEN, width.toFloat(), 0f, Color.YELLOW)
        g.fillOval(width / 4, height / 4, width / 2, height / 2)
        g.color = Color.WHITE
        g.font = g.font.deriveFont(height / 10f)
        g.drawString("${width}x${height}", width / 10, height / 2)
        g.dispose()
        return image
    }

    private fun saveTestImage(image: BufferedImage, name: String): FileReference {
        val file = File(outputDir, name)
        ImageIO.write(image, name.substringAfterLast('.'), file)
        return FileReference(file)
    }

    // by Claude - helper that wraps suspend compressed() with a wall-clock timeout
    private fun compressWithTimeout(ref: FileReference, maxWidth: Int = 2048, maxHeight: Int = 2048, quality: Float = 0.8f) =
        runBlocking { withTimeout(30.seconds) { ImageLocal(ref).compressed(maxWidth, maxHeight, quality) } }

    @Test
    fun compressLargeImageFitsWithinBounds() {
        val original = createTestImage(4000, 3000)
        val ref = saveTestImage(original, "large-original.png")
        val originalSize = ref.file.length()

        val result = compressWithTimeout(ref, maxWidth = 1024, maxHeight = 1024, quality = 0.8f)
        val resultBytes = runBlocking { result.data.toByteArray() }

        File(outputDir, "large-compressed.jpg").writeBytes(resultBytes)

        val decoded = ImageIO.read(resultBytes.inputStream())
        assertTrue(decoded.width <= 1024, "Width ${decoded.width} should be <= 1024")
        assertTrue(decoded.height <= 1024, "Height ${decoded.height} should be <= 1024")
        assertEquals(1024, decoded.width, "Width should be 1024 (landscape dominant)")
        assertEquals(768, decoded.height, "Height should be 768 (aspect ratio preserved)")
        assertTrue(resultBytes.size < originalSize, "Compressed ${resultBytes.size} should be < original $originalSize")
        println("Large: ${originalSize} bytes -> ${resultBytes.size} bytes (${resultBytes.size * 100 / originalSize}%)")
    }

    @Test
    fun compressAlreadySmallImageStillReencodes() {
        val original = createTestImage(200, 150)
        val ref = saveTestImage(original, "small-original.png")

        val result = compressWithTimeout(ref, maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = runBlocking { result.data.toByteArray() }

        File(outputDir, "small-compressed.jpg").writeBytes(resultBytes)

        val decoded = ImageIO.read(resultBytes.inputStream())
        assertEquals(200, decoded.width)
        assertEquals(150, decoded.height)
        println("Small: ${ref.file.length()} bytes -> ${resultBytes.size} bytes")
    }

    @Test
    fun compressTallPortraitImage() {
        val original = createTestImage(1000, 4000)
        val ref = saveTestImage(original, "portrait-original.png")

        val result = compressWithTimeout(ref, maxWidth = 1024, maxHeight = 1024, quality = 0.8f)
        val resultBytes = runBlocking { result.data.toByteArray() }

        File(outputDir, "portrait-compressed.jpg").writeBytes(resultBytes)

        val decoded = ImageIO.read(resultBytes.inputStream())
        assertTrue(decoded.width <= 1024)
        assertTrue(decoded.height <= 1024)
        assertEquals(256, decoded.width)
        assertEquals(1024, decoded.height)
        println("Portrait: ${ref.file.length()} bytes -> ${resultBytes.size} bytes")
    }

    @Test
    fun qualityAffectsFileSize() {
        val original = createTestImage(2000, 1500)
        val ref = saveTestImage(original, "quality-original.png")

        val lowQ = compressWithTimeout(ref, maxWidth = 1024, maxHeight = 1024, quality = 0.1f)
        val highQ = compressWithTimeout(ref, maxWidth = 1024, maxHeight = 1024, quality = 0.95f)

        val lowBytes = runBlocking { lowQ.data.toByteArray() }
        val highBytes = runBlocking { highQ.data.toByteArray() }

        File(outputDir, "quality-low.jpg").writeBytes(lowBytes)
        File(outputDir, "quality-high.jpg").writeBytes(highBytes)

        assertTrue(lowBytes.size < highBytes.size, "Low quality ${lowBytes.size} should be < high quality ${highBytes.size}")
        println("Quality comparison: low=${lowBytes.size} bytes, high=${highBytes.size} bytes")
    }

    @Test
    fun compressFromJpegSource() {
        val original = createTestImage(3000, 2000)
        val ref = saveTestImage(original, "jpeg-original.jpg")
        val originalSize = ref.file.length()

        val result = compressWithTimeout(ref, maxWidth = 800, maxHeight = 600, quality = 0.7f)
        val resultBytes = runBlocking { result.data.toByteArray() }

        File(outputDir, "jpeg-compressed.jpg").writeBytes(resultBytes)

        val decoded = ImageIO.read(resultBytes.inputStream())
        assertTrue(decoded.width <= 800)
        assertTrue(decoded.height <= 600)
        assertEquals(800, decoded.width)
        assertEquals(533, decoded.height)
        println("JPEG->JPEG: ${originalSize} bytes -> ${resultBytes.size} bytes")
    }

    @Test
    fun rejectsNonImageFile() {
        val textFile = File(outputDir, "not-an-image.txt").apply { writeText("hello world") }
        val ref = FileReference(textFile)

        try {
            compressWithTimeout(ref)
            assertTrue(false, "Should have thrown")
        } catch (e: IllegalArgumentException) {
            println("Correctly rejected non-image: ${e.message}")
        }
    }
}
