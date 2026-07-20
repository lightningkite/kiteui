package com.lightningkite.kiteui.models

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.Duration

public sealed interface Paint {
    public fun closestColor(): Color
    public fun map(mapper: (Color)->Color): Paint
}
public fun Paint.applyAlpha(alpha: Float): Paint = map { it.applyAlpha(alpha) }
public fun Paint.lighten(ratio: Float): Paint = map { it.lighten(ratio) }
public fun Paint.darken(ratio: Float): Paint = map { it.darken(ratio) }

@Serializable
public data class FadingColor(val base: Color, val alternate: Color): Paint {
    override fun closestColor(): Color = base
    override fun map(mapper: (Color) -> Color): Paint = FadingColor(base = mapper(base), alternate = mapper(alternate))
}
@Serializable
public data class GradientStop(val ratio: Float, val color: Color)
/**
 * Linear gradient paint.
 *
 * For canvas drawing, use [x0], [y0], [x1], [y1] to specify absolute start/end points.
 * When these are null, the gradient uses [angle] relative to the drawing bounds.
 */
public data class LinearGradient(
    val stops: List<GradientStop>,
    /**
     * Zero is left to right, angle added is clockwise.
     * Used when x0/y0/x1/y1 are not specified.
     */
    val angle: Angle = Angle.zero,
    val screenStatic: Boolean = false,
    /** Start point X coordinate for canvas gradients. */
    val x0: Double? = null,
    /** Start point Y coordinate for canvas gradients. */
    val y0: Double? = null,
    /** End point X coordinate for canvas gradients. */
    val x1: Double? = null,
    /** End point Y coordinate for canvas gradients. */
    val y1: Double? = null,
) : Paint {
    public companion object {
        public val INVALID: LinearGradient = LinearGradient(listOf())
    }
    override fun map(mapper: (Color) -> Color): Paint = copy(stops = stops.map { it.copy(color = it.color.let(mapper)) })
    override fun closestColor(): Color {
        if (stops.isEmpty()) return Color.transparent
        if (stops.size == 1) return stops[0].color
        return Color(
            alpha = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.alpha + b.color.alpha) / 2
            }.sum(),
            red = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.red + b.color.red) / 2
            }.sum(),
            green = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.green + b.color.green) / 2
            }.sum(),
            blue = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.blue + b.color.blue) / 2
            }.sum(),
        )
    }

    public fun toGrayscale(): LinearGradient = copy(stops = stops.map { it.copy(color = it.color.toGrayscale()) })
    public fun toWhite(ratio: Float): LinearGradient = copy(stops = stops.map { it.copy(color = it.color.toWhite(ratio)) })
    public fun toBlack(ratio: Float): LinearGradient = copy(stops = stops.map { it.copy(color = it.color.toBlack(ratio)) })
    public fun highlight(ratio: Float): LinearGradient = copy(stops = stops.map { it.copy(color = it.color.highlight(ratio)) })
    public fun invert(): LinearGradient = copy(stops = stops.map { it.copy(color = it.color.invert()) })
}

/**
 * Radial gradient paint.
 *
 * For canvas drawing, use [cx], [cy], [radius] to specify the gradient circle.
 * Optionally use [fx], [fy] for a focal point different from the center.
 * When these are null, the gradient centers in the drawing bounds.
 */
@Serializable
public data class RadialGradient(
    val stops: List<GradientStop>,
    val screenStatic: Boolean = false,
    /** Center X coordinate for canvas gradients. */
    val cx: Double? = null,
    /** Center Y coordinate for canvas gradients. */
    val cy: Double? = null,
    /** Radius for canvas gradients. */
    val radius: Double? = null,
    /** Focal point X coordinate (defaults to cx if null). */
    val fx: Double? = null,
    /** Focal point Y coordinate (defaults to cy if null). */
    val fy: Double? = null,
) : Paint {
    override fun map(mapper: (Color) -> Color): Paint = copy(stops = stops.map { it.copy(color = it.color.let(mapper)) })
    override fun closestColor(): Color {
        if (stops.isEmpty()) return Color.transparent
        if (stops.size == 1) return stops[0].color
        return Color(
            alpha = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.alpha + b.color.alpha) / 2
            }.sum(),
            red = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.red + b.color.red) / 2
            }.sum(),
            green = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.green + b.color.green) / 2
            }.sum(),
            blue = stops.asSequence().zipWithNext { a, b ->
                (b.ratio - a.ratio) * (a.color.blue + b.color.blue) / 2
            }.sum(),
        )
    }
}

@Serializable
public data class Color(
    val alpha: Float = 0f, val red: Float = 0f, val green: Float = 0f, val blue: Float = 0f
) : Paint {

    override fun map(mapper: (Color) -> Color): Paint = let(mapper)
    override fun closestColor(): Color = this
    public fun applyAlpha(alpha: Float): Color = copy(alpha = alpha * this.alpha)

    public fun toInt(): Int {
        return (alpha.byteize() shl 24) or (red.byteize() shl 16) or (green.byteize() shl 8) or (blue.byteize())
    }

    public fun toGradient(ratio: Float = 0.2f): LinearGradient = LinearGradient(
        stops = listOf(
            GradientStop(1f, this), GradientStop(0f, darken(ratio))
        )
    )

    public fun toGrayscale(): Color {
        val average = 0.299f * red + 0.587f * green + 0.114f * blue
        return Color(
            alpha = alpha, red = average, green = average, blue = average
        )
    }

    public fun darken(ratio: Float): Color = copy(
        red = red * (1f - ratio), green = green * (1f - ratio), blue = blue * (1f - ratio)
    )

    public fun lighten(ratio: Float): Color = copy(
        red = red + (1f - red) * ratio, green = green + (1f - green) * ratio, blue = blue + (1f - blue) * ratio
    )

    public fun withAlpha(alpha: Float): Color = copy(
        alpha = alpha
    )

    public companion object {

        public val transparent: Color = Color()
        public val white: Color = Color(1f, 1f, 1f, 1f)
        public val gray: Color = Color(1f, .5f, .5f, .5f)
        public fun gray(amount: Float): Color = Color(1f, amount, amount, amount)
        public val black: Color = Color(1f, 0f, 0f, 0f)

        public val red: Color = Color(1f, 1f, 0f, 0f)
        public val orange: Color = Color(1f, 1f, 0.5f, 0f)
        public val yellow: Color = Color(1f, 1f, 1f, 0f)
        public val green: Color = Color(1f, 0f, 1f, 0f)
        public val teal: Color = Color(1f, 0f, 1f, 1f)
        public val blue: Color = Color(1f, 0f, 0f, 1f)
        public val purple: Color = Color(1f, 1f, 0f, 1f)

        private fun Float.byteize() = (this * 0xFF).toInt().coerceIn(0x00, 0xFF)

        private fun Int.floatize() = (this.coerceIn(0x00, 0xFF).toFloat() / 0xFF)

        public fun fromInt(value: Int): Color = Color(
            alpha = value.ushr(24).and(0xFF).floatize(),
            red = value.shr(16).and(0xFF).floatize(),
            green = value.shr(8).and(0xFF).floatize(),
            blue = value.and(0xFF).floatize()
        )

        public fun fromHex(value: Int): Color = Color(
            alpha = 1f,
            red = value.shr(16).and(0xFF).floatize(),
            green = value.shr(8).and(0xFF).floatize(),
            blue = value.and(0xFF).floatize()
        )

        public fun fromHexString(value: String): Color = fromHex(value.replace("#", "").toInt(16))
        public fun fromRgbString(value: String): Color {
            val values = value
                .replace(")", "")
                .replace("rgba(", "")
                .replace("rgb(", "")
                .split(",", " ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return when {
                values.size >= 4 -> Color(
                    red = values[0].toInt().floatize(),
                    green = values[1].toInt().floatize(),
                    blue = values[2].toInt().floatize(),
                    alpha = values[3].toInt().floatize(),
                )
                values.size >= 3 -> Color(
                    red = values[0].toInt().floatize(),
                    green = values[1].toInt().floatize(),
                    blue = values[2].toInt().floatize(),
                )
                else -> transparent
            }
        }
        public fun interpolate(left: Color, right: Color, ratio: Float): Color {
            val invRatio = 1 - ratio
            return Color(
                alpha = left.alpha.times(invRatio) + right.alpha.times(ratio),
                red = left.red.times(invRatio) + right.red.times(ratio),
                green = left.green.times(invRatio) + right.green.times(ratio),
                blue = left.blue.times(invRatio) + right.blue.times(ratio)
            )
        }

        public fun hsvInterpolate(left: Color, right: Color, ratio: Float): Color =
            HSVColor.interpolate(left.toHSV(), right.toHSV(), ratio).toRGB()
    }

    val average: Float get() = (red + green + blue) / 3f
    val perceivedBrightness: Float
        get() = sqrt(
            red * red * HSPColor.redBrightness +
                    green * green * HSPColor.greenBrightness +
                    blue * blue * HSPColor.blueBrightness
        )
    val redInt: Int get() = red.byteize()
    val greenInt: Int get() = green.byteize()
    val blueInt: Int get() = blue.byteize()

    public operator fun plus(other: Color): Color = copy(
        red = (red + other.red),
        green = (green + other.green),
        blue = (blue + other.blue),
    )

    public operator fun minus(other: Color): Color = copy(
        red = (red - other.red),
        green = (green - other.green),
        blue = (blue - other.blue),
    )

    public operator fun div(other: Color): Color = copy(
        red = (red / other.red),
        green = (green / other.green),
        blue = (blue / other.blue),
    )

    public operator fun times(other: Color): Color = copy(
        red = (red * other.red),
        green = (green * other.green),
        blue = (blue * other.blue),
    )

    public infix fun channelDifferenceSum(other: Color): Float = abs(red - other.red) +
            abs(green - other.green) +
            abs(blue - other.blue) +
            abs(alpha - other.alpha)

    public fun toWhite(ratio: Float): Color = interpolate(this, white, ratio)
    public fun toBlack(ratio: Float): Color = interpolate(this, black, ratio)
    public fun highlight(ratio: Float): Color = if (average > .5) toBlack(ratio) else toWhite(ratio)
    public fun invert(): Color = Color(alpha = alpha, red = 1f - red, green = 1f - green, blue = 1f - blue)

    public fun toHSV(): HSVColor = HSVColor(alpha = alpha, hue = when {
        (red > green && red > blue) -> (green - blue).div(max(max(red, green), blue) - min(min(red, green), blue))
        (green > red && green > blue) -> (blue - red).div(
            max(max(red, green), blue) - min(
                min(red, green), blue
            )
        ).plus(2)

        (blue > green && blue > red) -> (red - green).div(
            max(max(red, green), blue) - min(
                min(red, green), blue
            )
        ).plus(4)

        else -> 0f
    }.let { Angle(it.plus(6f).rem(6f).div(6f)) }, saturation = run {
        val min = min(min(red, green), blue)
        val max = max(max(red, green), blue)
        if (max == 0f) 0f
        else (max - min) / max
    }, value = max(max(red, green), blue)
    )

    public fun toHSP(): HSPColor = HSPColor(alpha = alpha, hue = when {
        (red > green && red > blue) -> (green - blue).div(max(max(red, green), blue) - min(min(red, green), blue))
        (green > red && green > blue) -> (blue - red).div(
            max(max(red, green), blue) - min(
                min(red, green), blue
            )
        ).plus(2)

        (blue > green && blue > red) -> (red - green).div(
            max(max(red, green), blue) - min(
                min(red, green), blue
            )
        ).plus(4)

        else -> 0f
    }.let { Angle(it.plus(6f).rem(6f).div(6f)) }, saturation = run {
        val min = min(min(red, green), blue)
        val max = max(max(red, green), blue)
        if (max == 0f) 0f
        else (max - min) / max
    }, brightness = perceivedBrightness
    )

    public fun toWeb(): String {
        return "rgba($redInt, $greenInt, $blueInt, $alpha)"
    }

    public fun toAlphalessWeb(): String {
        @Suppress("EXPERIMENTAL_API_USAGE") return "#" + this.toInt().toUInt().toString(16).padStart(8, '0').drop(2)
    }
}

public interface ColorSpace {
    public fun toRGB(): Color
}

@Serializable
public data class HSVColor(
    val alpha: Float = 1f, val hue: Angle = Angle(0f), val saturation: Float = 0f, val value: Float = 0f
): ColorSpace {
    override fun toRGB(): Color {
        val h = (hue.turns.mod(1f) * 6).toInt()
        val f = hue.turns.mod(1f) * 6 - h
        val p = value.coerceIn(0f, 1f) * (1 - saturation.coerceIn(0f, 1f))
        val q = value.coerceIn(0f, 1f) * (1 - f * saturation.coerceIn(0f, 1f))
        val t = value.coerceIn(0f, 1f) * (1 - (1 - f) * saturation.coerceIn(0f, 1f))

        return when (h) {
            0 -> Color(alpha = alpha, red = value, green = t, blue = p)
            1 -> Color(alpha = alpha, red = q, green = value, blue = p)
            2 -> Color(alpha = alpha, red = p, green = value, blue = t)
            3 -> Color(alpha = alpha, red = p, green = q, blue = value)
            4 -> Color(alpha = alpha, red = t, green = p, blue = value)
            5 -> Color(alpha = alpha, red = value, green = p, blue = q)
            else -> Color.transparent
        }
    }

    public companion object {
        public fun interpolate(left: HSVColor, right: HSVColor, ratio: Float): HSVColor {
            val invRatio = 1 - ratio
//            val leftHuePower = left.saturation
//            val rightHuePower = right.saturation
//            val hueRatio = leftHuePower / (rightHuePower + leftHuePower)
            return HSVColor(
                alpha = left.alpha.times(invRatio) + right.alpha.times(ratio),
                hue = left.hue + (left.hue angleTo right.hue) * ratio,
                saturation = left.saturation.times(invRatio) + right.saturation.times(ratio),
                value = left.value.times(invRatio) + right.value.times(ratio)
            )
        }

        public fun fromRGB(color: Color): HSVColor {
            val r = color.red.coerceIn(0f, 1f)
            val g = color.green.coerceIn(0f, 1f)
            val b = color.blue.coerceIn(0f, 1f)

            val max = max(r, max(g, b))
            val min = min(r, min(g, b))
            val delta = max - min

            val value = max

            val saturation = if (max == 0f) {
                0f
            } else {
                delta / max
            }

            val hueRaw = when {
                delta == 0f -> 0f
                max == r -> (g - b) / delta
                max == g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }

            val hue = ((hueRaw / 6f) % 1f + 1f) % 1f

            return HSVColor(
                alpha = color.alpha,
                hue = Angle(hue),
                saturation = saturation,
                value = value
            )
        }
    }
}

@Serializable
public data class HSPColor(
    val alpha: Float = 1f, val hue: Angle = Angle(0f), val saturation: Float = 0f, val brightness: Float = 0f
): ColorSpace {
    override fun toRGB(): Color {
        val minOverMax = 1f - saturation
        var part: Float = 0f
        val r: Float
        val g: Float
        val b: Float
        var hue = this.hue.turns
        if (minOverMax > 0f) {
            if (hue < 1f / 6f) {   //  R>G>B
                hue = 6f * (hue - 0f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                b =
                    brightness / sqrt(redBrightness / minOverMax / minOverMax + greenBrightness * part * part + blueBrightness);
                r = (b) / minOverMax; g = (b) + hue * ((r) - (b)); } else if (hue < 2f / 6f) {   //  G>R>B
                hue = 6f * (-hue + 2f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                b =
                    brightness / sqrt(greenBrightness / minOverMax / minOverMax + redBrightness * part * part + blueBrightness);
                g = (b) / minOverMax; r = (b) + hue * ((g) - (b)); } else if (hue < 3f / 6f) {   //  G>B>R
                hue = 6f * (hue - 2f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                r =
                    brightness / sqrt(greenBrightness / minOverMax / minOverMax + blueBrightness * part * part + redBrightness);
                g = (r) / minOverMax; b = (r) + hue * ((g) - (r)); } else if (hue < 4f / 6f) {   //  B>G>R
                hue = 6f * (-hue + 4f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                r =
                    brightness / sqrt(blueBrightness / minOverMax / minOverMax + greenBrightness * part * part + redBrightness);
                b = (r) / minOverMax; g = (r) + hue * ((b) - (r)); } else if (hue < 5f / 6f) {   //  B>R>G
                hue = 6f * (hue - 4f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                g =
                    brightness / sqrt(blueBrightness / minOverMax / minOverMax + redBrightness * part * part + greenBrightness);
                b = (g) / minOverMax; r = (g) + hue * ((b) - (g)); } else {   //  R>B>G
                hue = 6f * (-hue + 6f / 6f); part = 1f + hue * (1f / minOverMax - 1f);
                g =
                    brightness / sqrt(redBrightness / minOverMax / minOverMax + blueBrightness * part * part + greenBrightness);
                r = (g) / minOverMax; b = (g) + hue * ((r) - (g)); }
        } else {
            if (hue < 1f / 6f) {   //  R>G>B
                hue = 6f * (hue - 0f / 6f); r =
                    sqrt(brightness * brightness / (redBrightness + greenBrightness * hue * hue)); g = (r) * hue; b =
                    0f; } else if (hue < 2f / 6f) {   //  G>R>B
                hue = 6f * (-hue + 2f / 6f); g =
                    sqrt(brightness * brightness / (greenBrightness + redBrightness * hue * hue)); r = (g) * hue; b =
                    0f; } else if (hue < 3f / 6f) {   //  G>B>R
                hue = 6f * (hue - 2f / 6f); g =
                    sqrt(brightness * brightness / (greenBrightness + blueBrightness * hue * hue)); b = (g) * hue; r =
                    0f; } else if (hue < 4f / 6f) {   //  B>G>R
                hue = 6f * (-hue + 4f / 6f); b =
                    sqrt(brightness * brightness / (blueBrightness + greenBrightness * hue * hue)); g = (b) * hue; r =
                    0f; } else if (hue < 5f / 6f) {   //  B>R>G
                hue = 6f * (hue - 4f / 6f); b =
                    sqrt(brightness * brightness / (blueBrightness + redBrightness * hue * hue)); r = (b) * hue; g =
                    0f; } else {   //  R>B>G
                hue = 6f * (-hue + 6f / 6f); r =
                    sqrt(brightness * brightness / (redBrightness + blueBrightness * hue * hue)); b = (r) * hue; g =
                    0f; }
        }
        return Color(red = r, green = g, blue = b, alpha = alpha)
    }

    public companion object {
        public const val redBrightness: Float = .299f
        public const val greenBrightness: Float = .587f
        public const val blueBrightness: Float = .114f
        public fun interpolate(left: HSPColor, right: HSPColor, ratio: Float): HSPColor {
            val invRatio = 1 - ratio
            return HSPColor(
                alpha = left.alpha.times(invRatio) + right.alpha.times(ratio),
                hue = left.hue + (left.hue angleTo right.hue) * ratio,
                saturation = left.saturation.times(invRatio) + right.saturation.times(ratio),
                brightness = left.brightness.times(invRatio) + right.brightness.times(ratio)
            )
        }

        public fun fromRGB(color: Color): HSPColor {
            val r = color.red
            val g = color.green
            val b = color.blue

            val alpha = color.alpha

            // --- Perceived brightness (P in HSP) ---
            val brightness = sqrt(
                r * r * redBrightness +
                        g * g * greenBrightness +
                        b * b * blueBrightness
            )

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            // --- Saturation ---
            val saturation = if (max == 0f) 0f else 1f - (min / max)

            // --- Hue ---
            val hueTurns = if (delta == 0f) {
                0f
            } else {
                when (max) {
                    r -> ((g - b) / delta).let {
                        val h = it / 6f
                        if (g < b) h + 1f else h
                    }
                    g -> ((b - r) / delta + 2f) / 6f
                    else -> ((r - g) / delta + 4f) / 6f
                }
            }

            return HSPColor(
                alpha = alpha,
                hue = Angle(hueTurns.mod(1f)),
                saturation = saturation.coerceIn(0f, 1f),
                brightness = brightness
            )
        }
    }
}


@Serializable
public data class HSLColor(
    val alpha: Float = 1f,
    val hue: Angle = Angle(0f),
    val saturation: Float = 0f,
    val lightness: Float = 0f
): ColorSpace {
    override fun toRGB(): Color {
        val h = hue.turns.mod(1f)
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        if (s == 0f) return Color(alpha = alpha, red = l, green = l, blue = l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        fun hue2rgb(p: Float, q: Float, t0: Float): Float {
            var t = t0
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }
        val r = hue2rgb(p, q, h + 1f / 3f)
        val g = hue2rgb(p, q, h)
        val b = hue2rgb(p, q, h - 1f / 3f)
        return Color(alpha = alpha, red = r, green = g, blue = b)
    }

    public companion object {
        public fun interpolate(left: HSLColor, right: HSLColor, ratio: Float): HSLColor {
            val inv = 1f - ratio
            return HSLColor(
                alpha = left.alpha * inv + right.alpha * ratio,
                hue = left.hue + (left.hue angleTo right.hue) * ratio,
                saturation = left.saturation * inv + right.saturation * ratio,
                lightness = left.lightness * inv + right.lightness * ratio
            )
        }
        public fun fromWeb(color: String): HSLColor {
            val items = color.substringAfter("(").substringBefore(")").split(",")
            return HSLColor(
                hue = items[0].toFloat().degrees,
                saturation = items[1].removeSuffix("%").toFloat().div(100),
                lightness = items[2].removeSuffix("%").toFloat().div(100),
                alpha = items.getOrNull(3)?.toFloat() ?: 1f,
            )
        }

        public fun fromRGB(color: Color): HSLColor {
            val r = color.red.coerceIn(0f, 1f)
            val g = color.green.coerceIn(0f, 1f)
            val b = color.blue.coerceIn(0f, 1f)

            val max = maxOf(r, maxOf(g, b))
            val min = minOf(r, minOf(g, b))
            val delta = max - min

            // 1. Calculate Lightness
            val l = (max + min) / 2f

            // 2. Calculate Saturation and Hue
            var h = 0f
            var s = 0f

            if (delta != 0f) {
                s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)

                h = when (max) {
                    r -> (g - b) / delta + (if (g < b) 6f else 0f)
                    g -> (b - r) / delta + 2f
                    else -> (r - g) / delta + 4f
                }
                h /= 6f // Normalize to 0..1 (turns)
            }

            return HSLColor(
                alpha = color.alpha,
                hue = h.turns, // Assuming .turns is an extension property for Angle
                saturation = s,
                lightness = l
            )
        }
    }
}

public fun Byte.positiveRemainder(other: Byte): Byte = this.rem(other).plus(other).rem(other).toByte()
public fun Short.positiveRemainder(other: Short): Short = this.rem(other).plus(other).rem(other).toShort()
public fun Int.positiveRemainder(other: Int): Int = this.rem(other).plus(other).rem(other)
public fun Long.positiveRemainder(other: Long): Long = this.rem(other).plus(other).rem(other)
public fun Float.positiveRemainder(other: Float): Float = this.rem(other).plus(other).rem(other)
public fun Double.positiveRemainder(other: Double): Double = this.rem(other).plus(other).rem(other)