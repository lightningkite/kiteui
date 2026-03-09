// by Claude
package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

actual suspend fun ImageLocal.compressed(
    maxWidth: Int,
    maxHeight: Int,
    quality: Float
): ImageRaw = withContext(Dispatchers.IO) {
    // Note: JVM ImageIO does not automatically handle EXIF orientation.
    // This is acceptable for server-side use where EXIF rotation is uncommon.
    val originalImage = ImageIO.read(file.file)
        ?: throw IllegalArgumentException("Could not decode image: ${file.file}")

    val (targetW, targetH) = calculateScaledSize(
        originalImage.width, originalImage.height, maxWidth, maxHeight
    )

    // Resize with high-quality bilinear interpolation
    val scaledImage = if (targetW == originalImage.width && targetH == originalImage.height) {
        // Still need to ensure TYPE_INT_RGB for JPEG output (no alpha channel)
        if (originalImage.type == BufferedImage.TYPE_INT_RGB) originalImage
        else {
            val rgb = BufferedImage(originalImage.width, originalImage.height, BufferedImage.TYPE_INT_RGB)
            val g = rgb.createGraphics()
            g.drawImage(originalImage, 0, 0, null)
            g.dispose()
            rgb
        }
    } else {
        val scaled = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
        val g2d = scaled.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.drawImage(originalImage, 0, 0, targetW, targetH, null)
        g2d.dispose()
        scaled
    }

    // Compress to JPEG with quality param
    val outputStream = ByteArrayOutputStream()
    val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
    val writeParam = jpegWriter.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = quality.coerceIn(0f, 1f)
    }
    jpegWriter.output = MemoryCacheImageOutputStream(outputStream)
    jpegWriter.write(null, IIOImage(scaledImage, null, null), writeParam)
    jpegWriter.dispose()

    ImageRaw(Blob(outputStream.toByteArray(), "image/jpeg"))
}
