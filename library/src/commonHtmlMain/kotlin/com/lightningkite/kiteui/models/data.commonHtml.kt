@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.encodeURIComponent

@InternalKiteUi
public actual data class DimensionRaw(
    public val px: Double = 0.0,
    public val rem: Double = 0.0,
): Comparable<DimensionRaw> {
    public val roughPx: Double get() = px + rem * 16
    public override fun toString(): String {
        return when {
            px == 0.0 -> "${rem}rem"
            rem == 0.0 -> "${px}px"
            else -> "calc(${px}px + ${rem}rem)"
        }
    }
    public override fun compareTo(other: DimensionRaw): Int = roughPx.compareTo(other.roughPx)
    public companion object {
        public val zero: DimensionRaw = DimensionRaw()
    }
}

@InternalKiteUi
public fun Dimension(
    px: Double = 0.0,
    rem: Double = 0.0,
): Dimension = Dimension(DimensionRaw(px, rem))

@InternalKiteUi
public actual val Int.px: Dimension
    get() = Dimension(px = this.toDouble())

@InternalKiteUi
public actual val Int.rem: Dimension
    get() = Dimension(rem = this.toDouble())

@InternalKiteUi
public actual val Double.rem: Dimension
    get() = Dimension(rem = this)

@InternalKiteUi
public actual val Int.dp: Dimension
    get() = Dimension(px = this.toDouble())

@InternalKiteUi
public actual val Double.dp: Dimension
    get() = Dimension(px = this)

@InternalKiteUi
public actual operator fun Dimension.plus(other: Dimension): Dimension = Dimension(
    px = this.value.px + other.value.px,
    rem = this.value.rem + other.value.rem,
)
@InternalKiteUi
public actual operator fun Dimension.minus(other: Dimension): Dimension = Dimension(
    px = this.value.px - other.value.px,
    rem = this.value.rem - other.value.rem,
)
@InternalKiteUi
public actual operator fun Dimension.times(other: Float): Dimension = Dimension(
    px = this.value.px * other,
    rem = this.value.rem * other,
)
@InternalKiteUi
public actual operator fun Dimension.div(other: Float): Dimension = Dimension(
    px = this.value.px / other,
    rem = this.value.rem / other,
)
@InternalKiteUi
public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = minOf(this, other)
@InternalKiteUi
public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = maxOf(this, other)

@InternalKiteUi
public fun CornerRadii.toRawCornerRadius(): String = when (this) {
    is CornerRadii.Constant -> "calc(min(var(--parentSpacing, 0px), ${value.value}))"
    is CornerRadii.ForceConstant -> value.value.toString()
    is CornerRadii.RatioOfSize -> "${ratio.times(100).toInt()}%"
    is CornerRadii.RatioOfSpacing -> "calc(var(--parentSpacing, 0px) * ${value})"
    is CornerRadii.PerCorner -> listOf(this.topLeft, this.topRight,  this.bottomRight, this.bottomLeft).joinToString(" ") {
        if (it) "${value.value}" else "0px"
    }
}

@InternalKiteUi
public actual data class Font(
    val cssFontFamilyName: String,
    val url: String? = null,
    val fallback: String = "Helvetica",
    val direct: FontDirect? = null,
)

@InternalKiteUi
public data class FontDirect(
    public val normal: Map<Int, String>,
    public val italics: Map<Int, String>,
)

@InternalKiteUi
public actual val systemDefaultFont: Font get() = Font("'Montserrat'", "https://fonts.googleapis.com/css2?family=Montserrat:wght@100;400;700&display=swap", "Helvetica")
@InternalKiteUi
public actual val systemDefaultFixedWidthFont: Font get() = Font("monospace")

@InternalKiteUi
public actual sealed class ImageSource actual constructor()
@InternalKiteUi
public actual data class ImageResource(val relativeUrl: String) : ImageSource()

@InternalKiteUi
public actual sealed class VideoSource actual constructor()
@InternalKiteUi
public actual data class VideoResource(val relativeUrl: String) : VideoSource()

@InternalKiteUi
public actual sealed class AudioSource actual constructor()
@InternalKiteUi
public actual data class AudioResource(val relativeUrl: String) : AudioSource()

@InternalKiteUi
public fun Dimension.toBoxShadow(): String {
    if (value.roughPx == 0.0)
        return "none"
    val offsetX = 0.px.value
    val offsetY = value
    val blur = 4.px.value
    val spread = 0.px.value
    return "$offsetX $offsetY $blur $spread #77777799"
}

public class ScreenTransitionPart(
    public val from: Map<String, String>,
    public val to: Map<String, String>
) {
    public operator fun plus(other: ScreenTransitionPart): ScreenTransitionPart = ScreenTransitionPart(from = from + other.from, to = to + other.to)
}

@InternalKiteUi
public actual class ScreenTransition(
    public val name: String,
    public val enter: ScreenTransitionPart,
    public val exit: ScreenTransitionPart,
) {
    public operator fun plus(other: ScreenTransition): ScreenTransition = ScreenTransition(name = name + other.name, enter = enter + other.enter, exit = exit + other.exit)
    public actual companion object {
        public actual val None: ScreenTransition = ScreenTransition(
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
        public actual val Push: ScreenTransition = ScreenTransition(
            name = "Push",
            enter = translate("X", 100, 0),
            exit = translate("X", 0, -100),
        )
        public actual val Pop: ScreenTransition = ScreenTransition(
            name = "Pop",
            enter = translate("X", -100, 0),
            exit = translate("X", 0, 100),
        )
        public actual val PullUp: ScreenTransition = ScreenTransition(
            name = "PullUp",
            enter = translate("Y", 100, 0),
            exit = translate("Y", 0, -100),
        )
        public actual val PullDown: ScreenTransition = ScreenTransition(
            name = "PullDown",
            enter = translate("Y", -100, 0),
            exit = translate("Y", 0, 100),
        )
        public actual val Fade: ScreenTransition = ScreenTransition(
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
        public actual val GrowFade: ScreenTransition = ScreenTransition(
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
        public actual val ShrinkFade: ScreenTransition = ScreenTransition(
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

@InternalKiteUi
public fun ImageVector.vectorToSvgDataUrl(): String {
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
                        else -> Color.transparent.toWeb()
                    }
                }\"/>"
            )
        }
        append("</svg>")
    })
}