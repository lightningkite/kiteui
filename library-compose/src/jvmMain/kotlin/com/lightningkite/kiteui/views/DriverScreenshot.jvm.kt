package com.lightningkite.kiteui.views

import androidx.compose.ui.graphics.toAwtImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A PNG of this element, cropped from the window's last frame. */
@OptIn(ExperimentalEncodingApi::class)
public actual suspend fun Element.driverScreenshot(): String {
    val root = checkNotNull(KiteUiRoot.current) { "No KiteUiRoot is showing" }
    val window = root.screenshot().toAwtImage()
    val bounds = checkNotNull(underlyingNativeElement.screenRectangle()) { "$this is not on screen" }
    val scale = root.density
    val left = (bounds.left * scale).toInt().coerceIn(0, window.width - 1)
    val top = (bounds.top * scale).toInt().coerceIn(0, window.height - 1)
    val width = ((bounds.right * scale).toInt() - left).coerceIn(1, window.width - left)
    val height = ((bounds.bottom * scale).toInt() - top).coerceIn(1, window.height - top)
    val out = ByteArrayOutputStream()
    ImageIO.write(window.getSubimage(left, top, width, height), "png", out)
    return Base64.encode(out.toByteArray())
}
