package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.*

abstract class ImageCompressionTest {

    abstract suspend fun createTestImage(width: Int, height: Int, name: String): FileReference
    abstract fun createNonImageFile(): FileReference
    abstract suspend fun ByteArray.getImageSize(): Size
    abstract suspend fun FileReference.getRawBytes(): ByteArray
    abstract suspend fun FileReference.getSize(): Long

    @Test
    fun compressedImageFitsImBounds() = runTest {
        val ref = createTestImage(4000, 3000, "large-original.png")
        val originalSize = ref.getSize()

        val result = ImageLocal(ref).compressed(maxWidth = 1024, maxHeight = 1024, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        val newSize = resultBytes.getImageSize()
        assertTrue(newSize.width <= 1024.0, "Width ${newSize.width} should be <= 1024")
        assertTrue(newSize.height <= 1024.0, "Height ${newSize.height} should be <= 1024")
        assertEquals(1024, newSize.width.toInt(), "Width should be 1024 (landscape dominant)")
        assertEquals(768, newSize.height.toInt(), "Height should be 768 (aspect ratio preserved)")
        assertTrue(resultBytes.size < originalSize, "Compressed ${resultBytes.size} should be < original $originalSize")
    }

    @Test
    fun compressSmallPNGStillReEncodes() = runTest {
        val ref = createTestImage(200, 150, "small-original.png")

        val result = ImageLocal(ref).compressed(maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertNotEquals(Base64.encode(ref.getRawBytes()), Base64.encode(resultBytes))

        val newSize = resultBytes.getImageSize()
        assertEquals(200.0, newSize.width)
        assertEquals(150.0, newSize.height)
    }

    @Test
    fun compressSmallJPGDoesNotReEncodes() = runTest {
        val ref = createTestImage(200, 150, "small-original.jpg")

        val result = ImageLocal(ref).compressed(maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertEquals(Base64.encode(ref.getRawBytes()), Base64.encode(resultBytes))
    }

    @Test
    fun compressTallPortraitImage() = runTest {
        val ref = createTestImage(1000, 4000, "portrait-original.png")

        val result = ImageLocal(ref).compressed(maxWidth = 1024, maxHeight = 1024, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        val newSize = resultBytes.getImageSize()
        assertTrue(newSize.width <= 1024.0)
        assertTrue(newSize.height <= 1024.0)
        assertEquals(256, newSize.width.toInt())
        assertEquals(1024, newSize.height.toInt())
    }

    @Test
    fun qualityAffectsFileSize() = runTest {
        val ref = createTestImage(2000, 1500, "quality-original.png")

        val lowQ = ImageLocal(ref).compressed(maxWidth = 1024, maxHeight = 1024, quality = 0.1f)
        val highQ = ImageLocal(ref).compressed(maxWidth = 1024, maxHeight = 1024, quality = 0.95f)

        val lowBytes = lowQ.data.toByteArray()
        val highBytes = highQ.data.toByteArray()

        assertTrue(
            lowBytes.size < highBytes.size,
            "Low quality ${lowBytes.size} should be < high quality ${highBytes.size}"
        )
    }

    @Test
    fun compressFromJpegSource() = runTest {
        val ref = createTestImage(3000, 2000, "jpeg-original.jpg")

        val result = ImageLocal(ref).compressed(maxWidth = 800, maxHeight = 600, quality = 0.7f)
        val resultBytes = result.data.toByteArray()

        val newSize = resultBytes.getImageSize()
        assertTrue(newSize.width <= 800.0)
        assertTrue(newSize.height <= 600.0)
        assertEquals(800, newSize.width.toInt())
        assertEquals(533, newSize.height.toInt())
    }

    @Test
    fun rejectsNonImageFile() = runTest {
        val ref = createNonImageFile()

        assertFailsWith<Exception> {
            ImageLocal(ref).compressed()
        }
    }
}
