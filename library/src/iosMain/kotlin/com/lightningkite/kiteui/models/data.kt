package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.toUIFontWeight
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.abs

// No reason to do a whole function call basic arithmetic

actual typealias DimensionRaw = Double
@Suppress("NOTHING_TO_INLINE") actual inline val Int.px: Dimension
    get() = Dimension(this.toDouble() / UIScreen.mainScreen.scale)

var remMultiplier: Double = 1.0

@Suppress("NOTHING_TO_INLINE") actual inline val Int.rem: Dimension
    get() = Dimension(this.toDouble() * UIFont.systemFontSize * remMultiplier)

@Suppress("NOTHING_TO_INLINE") actual inline val Double.rem: Dimension
    get() = Dimension(this * UIFont.systemFontSize * remMultiplier)

@Suppress("NOTHING_TO_INLINE") actual inline val Int.dp: Dimension
    get() = Dimension(this.toDouble())

@Suppress("NOTHING_TO_INLINE") actual inline val Double.dp: Dimension
    get() = Dimension(this)

@Suppress("NOTHING_TO_INLINE") actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value.plus(other.value))
@Suppress("NOTHING_TO_INLINE") actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value.minus(other.value))
@Suppress("NOTHING_TO_INLINE") actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value.times(other))
@Suppress("NOTHING_TO_INLINE") actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(this.value.div(other))
@Suppress("NOTHING_TO_INLINE") actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(this.value.coerceAtMost(other.value))
@Suppress("NOTHING_TO_INLINE") actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(this.value.coerceAtLeast(other.value))
actual val Dimension.px: Double get() = value * UIScreen.mainScreen.scale
actual val Dimension.canvasUnits: Double get() = value * UIScreen.mainScreen.scale
actual val Dimension.viewUnits: Double get() = value

actual data class Font(val get: (size: CGFloat, weight: UIFontWeight, italic: Boolean)->UIFont)
fun fontFromFamilyInfo(
    normal: String,
    italic: String?,
    bold: String?,
    boldItalic: String?
) = Font { size, weight, getItalic ->
    val fn = if(getItalic) {
        if(weight >= UIFontWeightBold) boldItalic ?: bold ?: italic ?: normal
        else italic ?: normal
    } else {
        if(weight >= UIFontWeightBold) bold ?: normal
        else normal
    }
    UIFont.fontWithName(fn, size) ?: systemDefaultFont.get(size, weight, getItalic)
}
fun fontFromFamilyInfo(
    normal: Map<Int, String>,
    italics: Map<Int, String>,
) = Font { size, weight, getItalic ->
    val fn = if(getItalic) {
        italics.entries.minByOrNull { abs(weight - it.key.toUIFontWeight()) }?.value
            ?: normal.entries.minBy { abs(weight - it.key.toUIFontWeight()) }.value
    } else {
        normal.entries.minBy { abs(weight - it.key.toUIFontWeight()) }.value
    }
    UIFont.fontWithName(fn, size) ?: systemDefaultFont.get(size, weight, getItalic)
}
actual val systemDefaultFont: Font get() = Font { size, weight, italic -> if(italic) UIFont.italicSystemFontOfSize(size) else UIFont.systemFontOfSize(size, weight) }
actual val systemDefaultFixedWidthFont: Font get() = Font { size, weight, italic -> UIFont.systemFontOfSize(size, weight) }

actual sealed class ImageSource actual constructor(): VisualMediaSource
actual data class ImageResource(val name: String) : ImageSource()
actual sealed class VideoSource actual constructor(): VisualMediaSource
actual data class VideoResource(val name: String, val extension: String) : VideoSource()
actual sealed class AudioSource actual constructor()
actual data class AudioResource(val name: String, val extension: String) : AudioSource()

