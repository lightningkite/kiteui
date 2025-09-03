package com.lightningkite.kiteui.models

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

actual val Dimension.px: Double get() = TODO()
actual val Dimension.canvasUnits: Double get() = TODO()
actual val Dimension.viewUnits: Double get() = TODO()

actual typealias Font = GenericFontFamily

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
    get() = TODO("Not yet implemented")

actual sealed class ImageSource actual constructor()
actual class ImageResource(val relativeUrl: String) : ImageSource()
actual sealed class VideoSource actual constructor()
actual class VideoResource(val relativeUrl: String) : VideoSource()
actual sealed class AudioSource actual constructor()
actual class AudioResource(val resource: String) : AudioSource()

actual typealias DimensionRaw = Float


actual val Int.px: Dimension
    get() = Dimension(this.dp.value) // In Compose, px is typically treated as dp for density-independent layout

actual val Int.rem: Dimension
    get() = Dimension(this.dp.value * 16) // 1 rem = 16dp (following web convention where 1rem = 16px)

actual val Int.dp: Dimension
    get() = Dimension(this.dp.value)

actual val Double.rem: Dimension
    get() = Dimension(this.dp.value * 16) // 1 rem = 16dp

actual val Double.dp: Dimension
    get() = Dimension(this.dp.value)

actual operator fun Dimension.plus(other: Dimension): Dimension {
    TODO("Not yet implemented")
}

actual operator fun Dimension.minus(other: Dimension): Dimension {
    TODO("Not yet implemented")
}

actual operator fun Dimension.times(other: Float): Dimension {
    TODO("Not yet implemented")
}

actual operator fun Dimension.div(other: Float): Dimension {
    TODO("Not yet implemented")
}

actual fun Dimension.coerceAtMost(other: Dimension): Dimension {
    TODO("Not yet implemented")
}

actual fun Dimension.coerceAtLeast(other: Dimension): Dimension {
    TODO("Not yet implemented")
}

