@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.encodeURIComponent

actual typealias DimensionRaw = String
actual val Int.px: Dimension
    get() = Dimension("${this}px")

actual val Int.rem: Dimension
    get() = Dimension("${this}rem")

actual val Double.rem: Dimension
    get() = Dimension("${this}rem")

actual val Int.dp: Dimension
    get() = Dimension("${this}px")

actual val Double.dp: Dimension
    get() = Dimension("${this}px")

actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension("calc(${this.value} + ${other.value})")
actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension("calc(${this.value} - ${other.value})")
actual inline operator fun Dimension.times(other: Float): Dimension = Dimension("calc(${this.value} * ${other})")
actual inline operator fun Dimension.div(other: Float): Dimension = Dimension("calc(${this.value} / ${other})")
actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension("calc(min(${this.value}, ${other.value}))")
actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension("calc(max(${this.value}, ${other.value}))")

fun CornerRadiusOptions.toRawCornerRadius(): DimensionRaw = when (this) {
    is CornerRadius.Constant -> "calc(min(var(--parentSpacing, 0px), ${value.value}))"
    is CornerRadius.ForceConstant -> value.value
    is CornerRadius.RatioOfSize -> "${ratio.times(100).toInt()}%"
    is CornerRadius.RatioOfSpacing -> "calc(var(--parentSpacing, 0px) * ${value})"
    is CornerRadii -> "${topStart.toRawCornerRadius()} ${topEnd.toRawCornerRadius()} ${bottomEnd.toRawCornerRadius()} ${bottomStart.toRawCornerRadius()}"
}

actual data class Font(
    val cssFontFamilyName: String,
    val url: String? = null,
    val fallback: String = "Helvetica",
    val direct: FontDirect? = null,
)

data class FontDirect(
    val normal: Map<Int, String>,
    val italics: Map<Int, String>,
)

actual val systemDefaultFont: Font get() = Font("'Montserrat'", "https://fonts.googleapis.com/css2?family=Montserrat:wght@100;400;700&display=swap", "Helvetica")
actual val systemDefaultFixedWidthFont: Font get() = Font("monospace")

actual sealed class ImageSource actual constructor()
actual data class ImageResource(val relativeUrl: String) : ImageSource()

actual sealed class VideoSource actual constructor()
actual data class VideoResource(val relativeUrl: String) : VideoSource()

actual sealed class AudioSource actual constructor()
actual data class AudioResource(val relativeUrl: String) : AudioSource()

fun Dimension.toBoxShadow(): String {
    if (value == "0px")
        return "none"
    val offsetX = 0.px.value
    val offsetY = value
    val blur = 4.px.value
    val spread = 0.px.value
    return "$offsetX $offsetY $blur $spread #77777799"
}

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

fun ImageVector.vectorToSvgDataUrl(): String {
    return "data:image/svg+xml;utf8," + encodeURIComponent(buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${width.value}\" height=\"${height.value}\" viewBox=\"$viewBoxMinX $viewBoxMinY $viewBoxWidth $viewBoxHeight\">")
        append("<defs>")
        paths.forEachIndexed { index: Int, path: ImageVector.Path ->
            when(val p = path.fillColor) {
                is LinearGradient -> {
                    append("<linearGradient id=\"fill$index\" gradientTransform=\"rotate(${p.angle.degrees}, 0.5, 0.5)\">")
                    for(stop in p.stops) {
                        append("<stop stop-color=\"${stop.color.toAlphalessWeb()}\" stop-opacity=\"${stop.color.alpha}\" offset=\"${stop.ratio.times(100).toInt()}%\"/>")
                    }
                    append("</linearGradient>")
                }
                is RadialGradient -> {
                    append("<radialGradient id=\"fill$index\">")
                    for(stop in p.stops) {
                        append("<stop stop-color=\"${stop.color.toAlphalessWeb()}\" stop-opacity=\"${stop.color.alpha}\" offset=\"${stop.ratio.times(100).toInt()}%\"/>")
                    }
                    append("</radialGradient>")
                }
                else -> {}
            }
        }
        append("</defs>")
        paths.forEachIndexed { index, path ->
            append(
                "<path fill-rule=\"evenodd\" d=\"${path.path}\" stroke=\"${path.strokeColor?.toWeb() ?: Color.transparent.toWeb()}\" stroke-width=\"${path.strokeWidth ?: 0}\" fill=\"${
                    when(val f = path.fillColor) {
                        is LinearGradient -> "url(#fill$index)"
                        is RadialGradient -> "url(#fill$index)"
                        is FadingColor -> f.base.toWeb()
                        is Color -> f.toWeb()
                        else -> Color.white.applyAlpha(0.0f).toWeb()
                    }
                }\"/>"
            )
        }
        append("</svg>")
    })
}