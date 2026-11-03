package com.lightningkite.kiteui.views

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.Font
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.text.platform.Font as ComposeFont

private val families = ConcurrentHashMap<Font, FontFamily>()

public actual fun Font.toFontFamily(): FontFamily = families.getOrPut(this) {
    val direct = direct
    when {
        direct != null -> FontFamily(
            direct.normal.map { (weight, path) -> ComposeFont(path, loadResource(path), FontWeight(weight), FontStyle.Normal) } +
                direct.italics.map { (weight, path) -> ComposeFont(path, loadResource(path), FontWeight(weight), FontStyle.Italic) }
        )
        cssFontFamilyName == "monospace" -> FontFamily.Monospace
        cssFontFamilyName == "system-ui" -> FontFamily.Default
        else -> {
            Log.warn("Font '$cssFontFamilyName' has no bundled font files, so the system font is used instead.")
            FontFamily.Default
        }
    }
}

internal fun loadResource(path: String): ByteArray =
    (Font::class.java.classLoader.getResourceAsStream(path) ?: error("Resource not found on the classpath: $path"))
        .use { it.readBytes() }
