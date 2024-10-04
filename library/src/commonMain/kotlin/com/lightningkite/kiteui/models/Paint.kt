package com.lightningkite.kiteui.models

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.Duration

sealed interface Paint {
    fun closestColor(): Color
    fun map(mapper: (Color)->Color): Paint
}
fun Paint.applyAlpha(alpha: Float): Paint = map { it.applyAlpha(alpha) }
fun Paint.lighten(ratio: Float): Paint = map { it.lighten(ratio) }
fun Paint.darken(ratio: Float): Paint = map { it.darken(ratio) }

data class FadingColor(val base: Color, val alternate: Color): Paint {
    override fun closestColor(): Color = base
    override fun map(mapper: (Color) -> Color): Paint = FadingColor(base = mapper(base), alternate = mapper(alternate))
}
data class GradientStop(val ratio: Float, val color: Color)
data class LinearGradient(
    val stops: List<GradientStop>,
    /**
     * Zero is left to right, angle added is clockwise
     */
    val angle: Angle = Angle.zero,
    val screenStatic: Boolean = false,
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

    fun toGrayscale() = copy(stops = stops.map { it.copy(color = it.color.toGrayscale()) })
    fun toWhite(ratio: Float) = copy(stops = stops.map { it.copy(color = it.color.toWhite(ratio)) })
    fun toBlack(ratio: Float) = copy(stops = stops.map { it.copy(color = it.color.toBlack(ratio)) })
    fun highlight(ratio: Float) = copy(stops = stops.map { it.copy(color = it.color.highlight(ratio)) })
    fun invert() = copy(stops = stops.map { it.copy(color = it.color.invert()) })
}

data class RadialGradient(
    val stops: List<GradientStop>,
    val screenStatic: Boolean = false,
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

data class Color(
    val alpha: Float = 0f, val red: Float = 0f, val green: Float = 0f, val blue: Float = 0f
) : Paint {

    override fun map(mapper: (Color) -> Color): Paint = let(mapper)
    override fun closestColor(): Color = this
    fun applyAlpha(alpha: Float) = copy(alpha = alpha * this.alpha)

    fun toInt(): Int {
        return (alpha.byteize() shl 24) or (red.byteize() shl 16) or (green.byteize() shl 8) or (blue.byteize())
    }

    fun toGradient(ratio: Float = 0.2f): LinearGradient = LinearGradient(
        stops = listOf(
            GradientStop(1f, this), GradientStop(0f, darken(ratio))
        )
    )

    fun toGrayscale(): Color {
        val average = 0.299f * red + 0.587f * green + 0.114f * blue
        return Color(
            alpha = alpha, red = average, green = average, blue = average
        )
    }

    fun darken(ratio: Float): Color = copy(
        red = red * (1f - ratio), green = green * (1f - ratio), blue = blue * (1f - ratio)
    )

    fun lighten(ratio: Float): Color = copy(
        red = red + (1f - red) * ratio, green = green + (1f - green) * ratio, blue = blue + (1f - blue) * ratio
    )

    fun withAlpha(alpha: Float): Color = copy(
        alpha = alpha
    )

    companion object {

        val transparent = Color()
        val white = Color(1f, 1f, 1f, 1f)
        val gray = Color(1f, .5f, .5f, .5f)
        fun gray(amount: Float) = Color(1f, amount, amount, amount)
        val black = Color(1f, 0f, 0f, 0f)

        val red = Color(1f, 1f, 0f, 0f)
        val orange = Color(1f, 1f, 0.5f, 0f)
        val yellow = Color(1f, 1f, 1f, 0f)
        val green = Color(1f, 0f, 1f, 0f)
        val teal = Color(1f, 0f, 1f, 1f)
        val blue = Color(1f, 0f, 0f, 1f)
        val purple = Color(1f, 1f, 0f, 1f)

        private fun Float.byteize() = (this * 0xFF).toInt().coerceIn(0x00, 0xFF)

        private fun Int.floatize() = (this.coerceIn(0x00, 0xFF).toFloat() / 0xFF)

        fun fromInt(value: Int): Color = Color(
            alpha = value.ushr(24).and(0xFF).floatize(),
            red = value.shr(16).and(0xFF).floatize(),
            green = value.shr(8).and(0xFF).floatize(),
            blue = value.and(0xFF).floatize()
        )

        fun fromHex(value: Int): Color = Color(
            alpha = 1f,
            red = value.shr(16).and(0xFF).floatize(),
            green = value.shr(8).and(0xFF).floatize(),
            blue = value.and(0xFF).floatize()
        )

        fun fromHexString(value: String): Color = fromHex(value.replace("#", "").toInt(16))

        fun interpolate(left: Color, right: Color, ratio: Float): Color {
            val invRatio = 1 - ratio
            return Color(
                alpha = left.alpha.times(invRatio) + right.alpha.times(ratio),
                red = left.red.times(invRatio) + right.red.times(ratio),
                green = left.green.times(invRatio) + right.green.times(ratio),
                blue = left.blue.times(invRatio) + right.blue.times(ratio)
            )
        }

        fun hsvInterpolate(left: Color, right: Color, ratio: Float): Color =
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

    operator fun plus(other: Color): Color = copy(
        red = (red + other.red),
        green = (green + other.green),
        blue = (blue + other.blue),
    )

    operator fun minus(other: Color): Color = copy(
        red = (red - other.red),
        green = (green - other.green),
        blue = (blue - other.blue),
    )

    operator fun div(other: Color): Color = copy(
        red = (red / other.red),
        green = (green / other.green),
        blue = (blue / other.blue),
    )

    operator fun times(other: Color): Color = copy(
        red = (red * other.red),
        green = (green * other.green),
        blue = (blue * other.blue),
    )

    infix fun channelDifferenceSum(other: Color): Float = abs(red - other.red) +
            abs(green - other.green) +
            abs(blue - other.blue) +
            abs(alpha - other.alpha)

    fun toWhite(ratio: Float) = interpolate(this, white, ratio)
    fun toBlack(ratio: Float) = interpolate(this, black, ratio)
    fun highlight(ratio: Float) = if (average > .5) toBlack(ratio) else toWhite(ratio)
    fun invert(): Color = Color(alpha = alpha, red = 1f - red, green = 1f - green, blue = 1f - blue)

    fun toHSV(): HSVColor = HSVColor(alpha = alpha, hue = when {
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

    fun toHSP(): HSPColor = HSPColor(alpha = alpha, hue = when {
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

    fun toWeb(): String {
        return "rgba($redInt, $greenInt, $blueInt, $alpha)"
    }

    fun toAlphalessWeb(): String {
        @Suppress("EXPERIMENTAL_API_USAGE") return "#" + this.toInt().toUInt().toString(16).padStart(8, '0').drop(2)
    }
}

data class HSVColor(
    val alpha: Float = 1f, val hue: Angle = Angle(0f), val saturation: Float = 0f, val value: Float = 0f
) {
    fun toRGB(): Color {
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

    companion object {
        fun interpolate(left: HSVColor, right: HSVColor, ratio: Float): HSVColor {
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
    }
}

data class HSPColor(
    val alpha: Float = 1f, val hue: Angle = Angle(0f), val saturation: Float = 0f, val brightness: Float = 0f
) {
    fun toRGB(): Color {
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

    companion object {
        const val redBrightness = .299f
        const val greenBrightness = .587f
        const val blueBrightness = .114f
        fun interpolate(left: HSPColor, right: HSPColor, ratio: Float): HSPColor {
            val invRatio = 1 - ratio
            return HSPColor(
                alpha = left.alpha.times(invRatio) + right.alpha.times(ratio),
                hue = left.hue + (left.hue angleTo right.hue) * ratio,
                saturation = left.saturation.times(invRatio) + right.saturation.times(ratio),
                brightness = left.brightness.times(invRatio) + right.brightness.times(ratio)
            )
        }
    }
}

fun Byte.positiveRemainder(other: Byte): Byte = this.rem(other).plus(other).rem(other).toByte()
fun Short.positiveRemainder(other: Short): Short = this.rem(other).plus(other).rem(other).toShort()
fun Int.positiveRemainder(other: Int): Int = this.rem(other).plus(other).rem(other)
fun Long.positiveRemainder(other: Long): Long = this.rem(other).plus(other).rem(other)
fun Float.positiveRemainder(other: Float): Float = this.rem(other).plus(other).rem(other)
fun Double.positiveRemainder(other: Double): Double = this.rem(other).plus(other).rem(other)