// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import kotlinx.cinterop.*
import kotlinx.coroutines.test.runTest
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageCompressionTest {

    /** Creates a test image as PNG bytes using Core Graphics. */
    private fun createTestImagePngBytes(width: Int, height: Int): ByteArray {
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
        CGContextFillEllipseInRect(ctx, CGRectMake(
            width / 4.0, height / 4.0,
            width / 2.0, height / 2.0
        ))

        val image = UIGraphicsGetImageFromCurrentImageContext()!!
        UIGraphicsEndImageContext()

        val pngData = UIImagePNGRepresentation(image)
            ?: throw IllegalStateException("Failed to create PNG data")
        return pngData.toByteArray()
    }

    private fun createTestFileReference(width: Int, height: Int): FileReference {
        val pngBytes = createTestImagePngBytes(width, height)
        return createFileReferenceFromBytes(pngBytes, "image/png", "test-${width}x${height}.png")
    }

    @Test
    fun compressLargeImageFitsWithinBounds() = runTest {
        val ref = createTestFileReference(2000, 1500)

        val result = ImageLocal(ref).compressed(maxWidth = 800, maxHeight = 800, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        assertTrue(resultBytes.isNotEmpty(), "Compressed result should not be empty")

        // Decode the output to verify dimensions
        val decoded = UIImage(data = result.data.data)
            ?: throw AssertionError("Could not decode compressed output")
        val w = decoded.size.useContents { width }.toInt()
        val h = decoded.size.useContents { height }.toInt()

        assertTrue(w <= 800, "Width $w should be <= 800")
        assertTrue(h <= 800, "Height $h should be <= 800")
        // 2000x1500 -> 800x600
        assertEquals(800, w, "Width should be 800")
        assertEquals(600, h, "Height should be 600")

        println("iOS large: -> ${resultBytes.size} bytes, ${w}x${h}")
    }

    @Test
    fun compressSmallImagePreservesDimensions() = runTest {
        val ref = createTestFileReference(100, 75)

        val result = ImageLocal(ref).compressed(maxWidth = 2048, maxHeight = 2048, quality = 0.8f)
        val resultBytes = result.data.toByteArray()
        assertTrue(resultBytes.isNotEmpty())

        val decoded = UIImage(data = result.data.data)!!
        val w = decoded.size.useContents { width }.toInt()
        val h = decoded.size.useContents { height }.toInt()

        assertEquals(100, w, "Width should be unchanged")
        assertEquals(75, h, "Height should be unchanged")
        println("iOS small: -> ${resultBytes.size} bytes, ${w}x${h}")
    }

    @Test
    fun compressPortraitImage() = runTest {
        val ref = createTestFileReference(500, 2000)

        val result = ImageLocal(ref).compressed(maxWidth = 512, maxHeight = 512, quality = 0.8f)
        val resultBytes = result.data.toByteArray()

        val decoded = UIImage(data = result.data.data)!!
        val w = decoded.size.useContents { width }.toInt()
        val h = decoded.size.useContents { height }.toInt()

        assertTrue(w <= 512)
        assertTrue(h <= 512)
        // 500x2000 -> 128x512
        assertEquals(128, w)
        assertEquals(512, h)
        println("iOS portrait: -> ${resultBytes.size} bytes, ${w}x${h}")
    }

    @Test
    fun qualityAffectsFileSize() = runTest {
        val ref = createTestFileReference(800, 600)

        val lowQ = ImageLocal(ref).compressed(maxWidth = 800, maxHeight = 600, quality = 0.1f)
        val highQ = ImageLocal(ref).compressed(maxWidth = 800, maxHeight = 600, quality = 0.95f)

        val lowBytes = lowQ.data.toByteArray()
        val highBytes = highQ.data.toByteArray()

        assertTrue(lowBytes.size < highBytes.size, "Low quality ${lowBytes.size} should be < high quality ${highBytes.size}")
        println("iOS quality: low=${lowBytes.size}, high=${highBytes.size}")
    }
}
