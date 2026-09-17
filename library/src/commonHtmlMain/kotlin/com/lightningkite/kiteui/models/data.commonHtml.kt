@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.models

import com.lightningkite.kotlinx.serialization.uri.encodeURIComponent

public actual data class DimensionRaw(
    val px: Double = 0.0,
    val rem: Double = 0.0,
) : Comparable<DimensionRaw> {
    val roughPx: Double get() = px + rem * 16
    override fun toString(): String {
        return when {
            px == 0.0 -> "${rem}rem"
            rem == 0.0 -> "${px}px"
            else -> "calc(${px}px + ${rem}rem)"
        }
    }

    override fun compareTo(other: DimensionRaw): Int = roughPx.compareTo(other.roughPx)

    public companion object {
        public val zero: DimensionRaw = DimensionRaw()
    }
}

public fun Dimension(
    px: Double = 0.0,
    rem: Double = 0.0,
): Dimension = Dimension(DimensionRaw(px, rem))

public actual val Int.px: Dimension
    get() = Dimension(px = this.toDouble())

public actual val Int.rem: Dimension
    get() = Dimension(rem = this.toDouble())

public actual val Double.rem: Dimension
    get() = Dimension(rem = this)

public actual val Int.dp: Dimension
    get() = Dimension(px = this.toDouble())

public actual val Double.dp: Dimension
    get() = Dimension(px = this)

public actual operator fun Dimension.plus(other: Dimension): Dimension = Dimension(
    px = this.value.px + other.value.px,
    rem = this.value.rem + other.value.rem,
)

public actual operator fun Dimension.minus(other: Dimension): Dimension = Dimension(
    px = this.value.px - other.value.px,
    rem = this.value.rem - other.value.rem,
)

public actual operator fun Dimension.times(other: Float): Dimension = Dimension(
    px = this.value.px * other,
    rem = this.value.rem * other,
)

public actual operator fun Dimension.div(other: Float): Dimension = Dimension(
    px = this.value.px / other,
    rem = this.value.rem / other,
)

public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = minOf(this, other)
public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = maxOf(this, other)

public fun CornerRadii.toRawCornerRadius(scaleCssVar: String? = null): String {
    fun String.scaled(): String = if (scaleCssVar == null) this else "calc(($this) * var($scaleCssVar))"
    return when (this) {
        is CornerRadii.AdaptiveToSpacing -> "calc(min(var(--parentSpacing, 0px), ${value.value}))".scaled()
        is CornerRadii.Fixed -> value.value.toString().scaled()
        is CornerRadii.RatioOfSize -> if (scaleCssVar == null) "${ratio.times(100).toInt()}%" else "calc(${ratio.times(100).toInt()}% * var($scaleCssVar))"
        is CornerRadii.RatioOfSpacing -> if (scaleCssVar == null) "calc(var(--parentSpacing, 0px) * $value)" else "calc(var(--parentSpacing, 0px) * $value * var($scaleCssVar))"
        is CornerRadii.PerCorner -> listOf(this.topLeft, this.topRight, this.bottomRight, this.bottomLeft).joinToString(" ") {
            if (it) value.value.toString().scaled() else "0px"
        }
    }
}

public actual data class Font(
    val cssFontFamilyName: String,
    val url: String? = null,
    val fallback: String = "Helvetica",
    val direct: FontDirect? = null,
)

public data class FontDirect(
    val normal: Map<Int, String>,
    val italics: Map<Int, String>,
)

public actual val systemDefaultFont: Font get() = Font("'Montserrat'", "https://fonts.googleapis.com/css2?family=Montserrat:wght@100;400;700&display=swap", "Helvetica")
public actual val systemDefaultFixedWidthFont: Font get() = Font("monospace")

public actual sealed class ImageSource actual constructor() : VisualMediaSource
public actual data class ImageResource(val relativeUrl: String) : ImageSource()

public actual sealed class VideoSource actual constructor() : VisualMediaSource
public actual data class VideoResource(val relativeUrl: String) : VideoSource()

public actual sealed class AudioSource actual constructor()
public actual data class AudioResource(val relativeUrl: String) : AudioSource()

public fun Dimension.toBoxShadow(): String {
    if (value.roughPx == 0.0)
        return "none"
    val offsetX = 0.px.value
    val offsetY = value
    val blur = 4.px.value
    val spread = 0.px.value
    return "$offsetX $offsetY $blur $spread #77777799"
}


public fun ImageVector.vectorToSvgDataUrl(): String {
    return "data:image/svg+xml;utf8," + encodeURIComponent(buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${width.value}\" height=\"${height.value}\" viewBox=\"$viewBoxMinX $viewBoxMinY $viewBoxWidth $viewBoxHeight\">")
        append("<defs>")
        paths.forEachIndexed { index: Int, path: ImageVector.Path ->
            when (val p = path.fillColor) {
                is LinearGradient -> {
                    append("<linearGradient id=\"fill$index\" gradientTransform=\"rotate(${p.angle.degrees}, 0.5, 0.5)\">")
                    for (stop in p.stops) {
                        append("<stop stop-color=\"${stop.color.toAlphalessWeb()}\" stop-opacity=\"${stop.color.alpha}\" offset=\"${stop.ratio.times(100).toInt()}%\"/>")
                    }
                    append("</linearGradient>")
                }

                is RadialGradient -> {
                    append("<radialGradient id=\"fill$index\">")
                    for (stop in p.stops) {
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
                    when (val f = path.fillColor) {
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