package com.lightningkite.kiteui.models

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.UIKit.*

actual typealias DimensionRaw = Double
actual val Int.px: Dimension
    get() = Dimension(this.toDouble() / UIScreen.mainScreen.scale)

actual val Int.rem: Dimension
    get() = Dimension(this.toDouble() * UIFont.systemFontSize)

actual val Double.rem: Dimension
    get() = Dimension(this * UIFont.systemFontSize)

actual val Int.dp: Dimension
    get() = Dimension(this.toDouble())

actual val Double.dp: Dimension
    get() = Dimension(this)

actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value.plus(other.value))
actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value.minus(other.value))
actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value.times(other))
actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(this.value.div(other))
actual val Dimension.px: Double get() = value * UIScreen.mainScreen.scale

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
actual val systemDefaultFont: Font get() = Font { size, weight, italic -> if(italic) UIFont.italicSystemFontOfSize(size) else UIFont.systemFontOfSize(size, weight) }
actual val systemDefaultFixedWidthFont: Font get() = Font { size, weight, italic -> UIFont.systemFontOfSize(size, weight) }

actual sealed class ImageSource actual constructor()
actual class ImageResource(val name: String) : ImageSource()
actual sealed class VideoSource actual constructor()
actual class VideoResource(val name: String, val extension: String) : VideoSource()
actual sealed class AudioSource actual constructor()
actual class AudioResource(val name: String, val extension: String) : AudioSource()

class ScreenTransitionPart(
    val from: UIView.()->Unit,
    val to: UIView.()->Unit
) {
    operator fun plus(other: ScreenTransitionPart) = ScreenTransitionPart(from = { from(this); other.from(this) }, to = { to(this); other.to(this) })
}

actual class ScreenTransition(
    val name: String,
    val enter: ScreenTransitionPart,
    val exit: ScreenTransitionPart,
) {
    operator fun plus(other: ScreenTransition) = ScreenTransition(name = name + other.name, enter = enter + other.enter, exit = exit + other.exit)
    actual companion object {
        actual val None: ScreenTransition = ScreenTransition(
            name = "None",
            enter = ScreenTransitionPart(
                from = {},
                to = {},
            ),
            exit = ScreenTransitionPart(
                from = {},
                to = {},
            ),
        )
        @OptIn(ExperimentalForeignApi::class)
        private fun translateX(ratio: CGFloat): UIView.()->Unit {
            return { transform = CGAffineTransformMakeTranslation((superview?.bounds?.useContents { size.width } ?: bounds?.useContents { size.width } ?: 0.0) * ratio, 0.0) }
        }
        @OptIn(ExperimentalForeignApi::class)
        private fun translateY(ratio: CGFloat): UIView.()->Unit {
            return { transform = CGAffineTransformMakeTranslation((superview?.bounds?.useContents { size.width } ?: bounds?.useContents { size.width } ?: 0.0) * ratio, 0.0) }
        }
        private fun translateX(fromPercent: Int, toPercent: Int) = ScreenTransitionPart(
            from = translateX(fromPercent / 100.0),
            to = translateX(toPercent / 100.0),
        )
        private fun translateY(fromPercent: Int, toPercent: Int) = ScreenTransitionPart(
            from = translateY(fromPercent / 100.0),
            to = translateY(toPercent / 100.0),
        )
        actual val Push: ScreenTransition = ScreenTransition(
            name = "Push",
            enter = translateX(100, 0),
            exit = translateX(0, -100),
        )
        actual val Pop: ScreenTransition = ScreenTransition(
            name = "Pop",
            enter = translateX(-100, 0),
            exit = translateX(0, 100),
        )
        actual val PullUp: ScreenTransition = ScreenTransition(
            name = "PullUp",
            enter = translateY(100, 0),
            exit = translateY(0, -100),
        )
        actual val PullDown: ScreenTransition = ScreenTransition(
            name = "PullDown",
            enter = translateY(-100, 0),
            exit = translateY(0, 100),
        )
        actual val Fade: ScreenTransition = ScreenTransition(
            name = "Fade",
            enter = ScreenTransitionPart(
                from = { alpha = 0.0 },
                to = { alpha = 1.0 },
            ),
            exit = ScreenTransitionPart(
                from = { alpha = 1.0 },
                to = { alpha = 0.0 },
            ),
        )
        @OptIn(ExperimentalForeignApi::class)
        private val sizeNeutral: UIView.()->Unit = { transform = CGAffineTransformMakeTranslation(0.0, 0.0) }
        @OptIn(ExperimentalForeignApi::class)
        private val sizeLarge: UIView.()->Unit = { transform = CGAffineTransformMakeScale(1.33, 1.33).let { CGAffineTransformTranslate(it, 0.0, -100.0) } }
        @OptIn(ExperimentalForeignApi::class)
        private val sizeSmall: UIView.()->Unit = { transform = CGAffineTransformMakeScale(0.75, 0.75).let { CGAffineTransformTranslate(it, 0.0, 100.0) } }
        actual val GrowFade: ScreenTransition = ScreenTransition(
            name = "Grow",
            enter = ScreenTransitionPart(
                from = sizeSmall,
                to = sizeNeutral,
            ),
            exit = ScreenTransitionPart(
                from = sizeNeutral,
                to = sizeLarge,
            ),
        ) + Fade
        actual val ShrinkFade: ScreenTransition = ScreenTransition(
            name = "Shrink",
            enter = ScreenTransitionPart(
                from = sizeLarge,
                to = sizeNeutral,
            ),
            exit = ScreenTransitionPart(
                from = sizeNeutral,
                to = sizeSmall,
            ),
        ) + Fade
    }
}