package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Size
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageCompressionTestSSR: ImageCompressionTest() {

    init {
        // by Claude - AWT hangs without headless mode when system graphics services are unavailable
        System.setProperty("java.awt.headless", "true")
    }

    private val outputDir = File("build/test-output/image-compression").apply { mkdirs() }

    override suspend fun createTestImage(width: Int, height: Int, name: String): FileReference {
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
        val file = File(outputDir, name)
        ImageIO.write(image, name.substringAfterLast('.'), file)
        return FileReference(file)
    }

    override fun createNonImageFile(): FileReference = FileReference(File(outputDir, "not-an-image.txt").apply { writeText("hello world") })

    override suspend fun ByteArray.getImageSize(): Size = ImageIO.read(this.inputStream()).let{
        Size(it.width.toDouble(), it.height.toDouble())
    }

    override suspend fun FileReference.getRawBytes(): ByteArray = this.file.readBytes()
    override suspend fun FileReference.getSize(): Long = this.bytes()

}
