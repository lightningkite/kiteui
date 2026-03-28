package com.lightningkite.kiteui.views

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

actual suspend fun RView.driverScreenshot(): String {
    val component = native
    val width = component.width.coerceAtLeast(1)
    val height = component.height.coerceAtLeast(1)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        component.paint(g)
    } finally {
        g.dispose()
    }
    val baos = ByteArrayOutputStream()
    ImageIO.write(image, "png", baos)
    return Base64.getEncoder().encodeToString(baos.toByteArray())
}
