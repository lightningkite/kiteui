package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.toUIFontWeight
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.abs
import kotlin.math.min

// No reason to do a whole function call basic arithmetic

public actual typealias DimensionRaw = Double
@Suppress("NOTHING_TO_INLINE") public actual inline val Int.px: Dimension
    get() = Dimension(this.toDouble() / UIScreen.mainScreen.scale)

@InternalKiteUi
public var remMultiplier: Double = 1.0

@Suppress("NOTHING_TO_INLINE") public actual inline val Int.rem: Dimension
    get() = Dimension(this.toDouble() * UIFont.systemFontSize * remMultiplier)

@Suppress("NOTHING_TO_INLINE") public actual inline val Double.rem: Dimension
    get() = Dimension(this * UIFont.systemFontSize * remMultiplier)

@Suppress("NOTHING_TO_INLINE") public actual inline val Int.dp: Dimension
    get() = Dimension(this.toDouble())

@Suppress("NOTHING_TO_INLINE") public actual inline val Double.dp: Dimension
    get() = Dimension(this)

@Suppress("NOTHING_TO_INLINE") public actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value.plus(other.value))
@Suppress("NOTHING_TO_INLINE") public actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value.minus(other.value))
@Suppress("NOTHING_TO_INLINE") public actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value.times(other))
@Suppress("NOTHING_TO_INLINE") public actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(this.value.div(other))
@Suppress("NOTHING_TO_INLINE") public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(this.value.coerceAtMost(other.value))
@Suppress("NOTHING_TO_INLINE") public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(this.value.coerceAtLeast(other.value))
public actual val Dimension.px: Double get() = value * UIScreen.mainScreen.scale
public actual val Dimension.canvasUnits: Double get() = value

public actual data class Font(val get: (size: CGFloat, weight: UIFontWeight, italic: Boolean)->UIFont)

@InternalKiteUi
public fun fontFromFamilyInfo(
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
@InternalKiteUi
public fun fontFromFamilyInfo(
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
public actual val systemDefaultFont: Font get() = Font { size, weight, italic -> if(italic) UIFont.italicSystemFontOfSize(size) else UIFont.systemFontOfSize(size, weight) }
public actual val systemDefaultFixedWidthFont: Font get() = Font { size, weight, italic -> UIFont.systemFontOfSize(size, weight) }

public actual sealed class ImageSource public actual constructor()
public actual data class ImageResource(val name: String) : ImageSource()
public actual sealed class VideoSource public actual constructor()
public actual data class VideoResource(val name: String, val extension: String) : VideoSource()
public actual sealed class AudioSource public actual constructor()
public actual data class AudioResource(val name: String, val extension: String) : AudioSource()

public actual class ScreenTransition(
    val name: String,
    val enter: UIView.()->Unit,
    val exit: UIView.()->Unit,
) {
    operator fun plus(other: ScreenTransition) = ScreenTransition(name = name + other.name, enter = { enter(this); other.enter(this) }, exit = { exit(this); other.exit(this) })
    public actual companion object {
        public actual val None: ScreenTransition = ScreenTransition(
            name = "None",
            enter = {},
            exit = {},
        )

        private fun translateX(ratio: CGFloat): UIView.()->Unit {
//            return { transform = CGAffineTransformMakeTranslation(ratio * 100.0, 0.0) }
            return { transform = CGAffineTransformMakeTranslation((superview?.bounds?.useContents { size.width } ?: bounds?.useContents { size.width } ?: 0.0) * ratio, 0.0) }
        }

        private fun translateY(ratio: CGFloat): UIView.()->Unit {
            return { transform = CGAffineTransformMakeTranslation(0.0, (superview?.bounds?.useContents { size.height } ?: bounds?.useContents { size.height } ?: 0.0) * ratio) }
        }

        public actual val Push: ScreenTransition = ScreenTransition(
            name = "Push",
            enter = translateX(1.0),
            exit = translateX(-1.0),
        )
        public actual val Pop: ScreenTransition = ScreenTransition(
            name = "Pop",
            enter = translateX(-1.0),
            exit = translateX(1.0),
        )
        public actual val PullUp: ScreenTransition = ScreenTransition(
            name = "PullUp",
            enter = translateY(1.0),
            exit = translateY(1.0),
        )
        public actual val PullDown: ScreenTransition = ScreenTransition(
            name = "PullDown",
            enter = translateY(-1.0),
            exit = translateY(1.0),
        )
        public actual val Fade: ScreenTransition = ScreenTransition(
            name = "Fade",
            enter = { alpha = 0.0 },
            exit = { alpha = 0.0 },
        )

        private val sizeNeutral: UIView.()->Unit = { transform = CGAffineTransformMakeTranslation(0.0, 0.0) }

        private val sizeLarge: UIView.()->Unit = { transform = CGAffineTransformMakeScale(1.33, 1.33).let { CGAffineTransformTranslate(it, 0.0, -100.0) } }

        private val sizeSmall: UIView.()->Unit = { transform = CGAffineTransformMakeScale(0.75, 0.75).let { CGAffineTransformTranslate(it, 0.0, 100.0) } }
        public actual val GrowFade: ScreenTransition = ScreenTransition(
            name = "Grow",
            enter = sizeSmall,
            exit = sizeLarge,
        ) + Fade
        public actual val ShrinkFade: ScreenTransition = ScreenTransition(
            name = "Shrink",
            enter = sizeLarge,
            exit = sizeSmall,
        ) + Fade
    }
}