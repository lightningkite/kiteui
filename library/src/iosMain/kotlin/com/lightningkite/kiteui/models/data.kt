package com.lightningkite.kiteui.models

import platform.CoreGraphics.CGFloat
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
    val from: Map<String, String>,
    val to: Map<String, String>
) {
    operator fun plus(other: ScreenTransitionPart) = ScreenTransitionPart(from = from + other.from, to = to + other.to)
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
                from = mapOf(),
                to = mapOf(),
            ),
            exit = ScreenTransitionPart(
                from = mapOf(),
                to = mapOf(),
            ),
        )
        private fun translate(dir: String, from: Int, to: Int) = ScreenTransitionPart(
            from = mapOf("transform" to "translate$dir(${from}%)"),
            to = mapOf("transform" to "translate$dir(${to}%)"),
        )
        actual val Push: ScreenTransition = ScreenTransition(
            name = "Push",
            enter = translate("X", 100, 0),
            exit = translate("X", 0, -100),
        )
        actual val Pop: ScreenTransition = ScreenTransition(
            name = "Pop",
            enter = translate("X", -100, 0),
            exit = translate("X", 0, 100),
        )
        actual val PullUp: ScreenTransition = ScreenTransition(
            name = "PullUp",
            enter = translate("Y", 100, 0),
            exit = translate("Y", 0, -100),
        )
        actual val PullDown: ScreenTransition = ScreenTransition(
            name = "PullDown",
            enter = translate("Y", -100, 0),
            exit = translate("Y", 0, 100),
        )
        actual val Fade: ScreenTransition = ScreenTransition(
            name = "Fade",
            enter = ScreenTransitionPart(
                from = mapOf("opacity" to "0"),
                to = mapOf("opacity" to "1"),
            ),
            exit = ScreenTransitionPart(
                from = mapOf("opacity" to "1"),
                to = mapOf("opacity" to "0"),
            ),
        )
        actual val GrowFade: ScreenTransition = ScreenTransition(
            name = "Grow",
            enter = ScreenTransitionPart(
                from = mapOf("transform" to "scale(0.75) translateY(7vh)"),
                to = mapOf("transform" to "scale(1.0) translateY(0)"),
            ),
            exit = ScreenTransitionPart(
                from = mapOf("transform" to "scale(1.00) translateY(0)"),
                to = mapOf("transform" to "scale(1.33) translateY(-7vh)"),
            ),
        ) + Fade
        actual val ShrinkFade: ScreenTransition = ScreenTransition(
            name = "Shrink",
            enter = ScreenTransitionPart(
                from = mapOf("transform" to "scale(1.33) translateY(-7vh)"),
                to = mapOf("transform" to "scale(1.0) translateY(0)"),
            ),
            exit = ScreenTransitionPart(
                from = mapOf("transform" to "scale(1.0) translateY(0vh)"),
                to = mapOf("transform" to "scale(0.75) translateY(7vh)"),
            ),
        ) + Fade
    }
}