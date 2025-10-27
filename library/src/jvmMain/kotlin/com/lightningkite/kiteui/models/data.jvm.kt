package com.lightningkite.kiteui.models

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

actual val Dimension.px: Double get() = value.toDouble()
actual val Dimension.canvasUnits: Double get() = value.toDouble()
actual val Dimension.viewUnits: Double get() = value.toDouble()

actual typealias Font = GenericFontFamily
fun Font(
    cssFontFamilyName: String,
    direct: FontDirect,
): Font = FontFamily.SansSerif

data class FontDirect(
    val normal: Map<Int, String>,
    val italics: Map<Int, String>,
)

fun Align.toComposeAlign(): TextAlign {
    return when (this) {
        Align.Start -> TextAlign.Start
        Align.End -> TextAlign.End
        Align.Center -> TextAlign.Center
        Align.Stretch -> TextAlign.Justify
    }
}

actual val systemDefaultFont: Font
    get() = FontFamily.SansSerif
actual val systemDefaultFixedWidthFont: Font
    get() = FontFamily.Monospace

actual sealed class ImageSource actual constructor(): VisualMediaSource
actual class ImageResource(val relativeUrl: String) : ImageSource()
actual sealed class VideoSource actual constructor(): VisualMediaSource
actual class VideoResource(val relativeUrl: String) : VideoSource()
actual sealed class AudioSource actual constructor()
actual class AudioResource(val resource: String) : AudioSource()

actual typealias DimensionRaw = Float


actual val Int.px: Dimension
    get() = Dimension(this.toFloat())

actual val Int.rem: Dimension
    get() = Dimension(this * 16f)

actual val Int.dp: Dimension
    get() = Dimension(this.toFloat())

actual val Double.rem: Dimension
    get() = Dimension((this * 16).toFloat())

actual val Double.dp: Dimension
    get() = Dimension(this.toFloat())

actual operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value + other.value)

actual operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value - other.value)

actual operator fun Dimension.times(other: Float): Dimension = Dimension(this.value * other)

actual operator fun Dimension.div(other: Float): Dimension = Dimension(if (other != 0f) this.value / other else 0f)

actual fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(this.value.coerceAtMost(other.value))

actual fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(this.value.coerceAtLeast(other.value))

