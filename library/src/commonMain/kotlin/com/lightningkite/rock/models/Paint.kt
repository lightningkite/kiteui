package com.lightningkite.rock.models

import com.lightningkite.rock.Angle
import kotlin.math.max
import kotlin.math.min

sealed interface Paint {
    fun closestColor(): Color
}

data class GradientStop(val ratio: Float, val color: Color)
data class LinearGradient(
    val stops: List<GradientStop>,
    val angle: Angle = Angle.zero,
    val screenStatic: Boolean = false,
) : Paint {
    override fun closestColor(): Color {
        return stops.maxByOrNull { it.ratio }?.color ?: Color.transparent
    }
}

data class RadialGradient(
    val stops: List<GradientStop>,
    val screenStatic: Boolean = false,
) : Paint {
    override fun closestColor(): Color {
        return stops.maxByOrNull { it.ratio }?.color ?: Color.transparent
    }
}

data class Color(
    val alpha: Float = 0f,
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f
) : Paint {

    override fun closestColor(): Color = this

    fun toInt(): Int {
        return (alpha.byteize() shl 24) or (red.byteize() shl 16) or (green.byteize() shl 8) or (blue.byteize())
    }

    fun toGradient(ratio: Float = 0.2f): LinearGradient = LinearGradient(
        stops = listOf(
            GradientStop(1f, this),
            GradientStop(0f, darken(ratio))
        )
    )

    fun toGrayscale(): Color {
        val average = 0.299f * red + 0.587f * green + 0.114f * blue
        return Color(
            alpha = alpha,
            red = average,
            green = average,
            blue = average
        )
    }

    fun darken(ratio: Float): Color = copy(
        red = red * (1f - ratio),
        green = green * (1f - ratio),
        blue = blue * (1f - ratio)
    )

    fun lighten(ratio: Float): Color = copy(
        red = red + (1f - red) * ratio,
        green = green + (1f - green) * ratio,
        blue = blue + (1f - blue) * ratio
    )

    companion object {

        val transparent = Color()
        val white = Color(1f, 1f, 1f, 1f)
        val gray = Color(1f, .5f, .5f, .5f)
        fun gray(amount: Float) = Color(1f, amount, amount, amount)
        val black = Color(1f, 0f, 0f, 0f)

        val red = Color(1f, 1f, 0f, 0f)
        val yellow = Color(1f, 1f, 1f, 0f)
        val green = Color(1f, 0f, 1f, 0f)
        val teal = Color(1f, 0f, 1f, 1f)
        val blue = Color(1f, 0f, 0f, 1f)
        val purple = Color(1f, 1f, 0f, 1f)

        private fun Float.byteize() = (this * 0xFF).toInt()

        private fun Int.floatize() = (this.toFloat() / 0xFF)

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
    val redInt: Int get() = red.byteize()
    val greenInt: Int get() = green.byteize()
    val blueInt: Int get() = blue.byteize()

    operator fun plus(other: Color): Color = copy(
        red = (red + other.red).coerceIn(0f, 1f),
        green = (green + other.green).coerceIn(0f, 1f),
        blue = (blue + other.blue).coerceIn(0f, 1f)
    )

    operator fun minus(other: Color): Color = copy(
        red = (red - other.red).coerceIn(0f, 1f),
        green = (green - other.green).coerceIn(0f, 1f),
        blue = (blue - other.blue).coerceIn(0f, 1f)
    )

    operator fun div(other: Color): Color = copy(
        red = (red / other.red).coerceIn(0f, 1f),
        green = (green / other.green).coerceIn(0f, 1f),
        blue = (blue / other.blue).coerceIn(0f, 1f)
    )

    operator fun times(other: Color): Color = copy(
        red = (red * other.red).coerceIn(0f, 1f),
        green = (green * other.green).coerceIn(0f, 1f),
        blue = (blue * other.blue).coerceIn(0f, 1f)
    )

    fun toWhite(ratio: Float) = interpolate(this, white, ratio)
    fun toBlack(ratio: Float) = interpolate(this, black, ratio)
    fun highlight(ratio: Float) = if (average > .5) toBlack(ratio) else toWhite(ratio)

    fun toHSV(): HSVColor = HSVColor(
        alpha = alpha,
        hue = when {
            (red > green && red > blue) -> (green - blue).div(max(max(red, green), blue) - min(min(red, green), blue))
            (green > red && green > blue) -> (blue - red).div(
                max(max(red, green), blue) - min(
                    min(red, green),
                    blue
                )
            ).plus(2)

            (blue > green && blue > red) -> (red - green).div(
                max(max(red, green), blue) - min(
                    min(red, green),
                    blue
                )
            ).plus(4)

            else -> 0f
        }.let { Angle(it.plus(6f).rem(6f).div(6f)) },
        saturation = run {
            val min = min(min(red, green), blue)
            val max = max(max(red, green), blue)
            if (max == 0f) 0f
            else (max - min) / max
        },
        value = max(max(red, green), blue)
    )

    fun toWeb(): String {
        return "rgba($redInt, $greenInt, $blueInt, $alpha)"
    }

    fun toAlphalessWeb(): String {
        @Suppress("EXPERIMENTAL_API_USAGE")
        return "#" + this.toInt().toUInt().toString(16).padStart(8, '0').drop(2)
    }
}

data class HSVColor(
    val alpha: Float = 0f,
    val hue: Angle = Angle(0f),
    val saturation: Float = 0f,
    val value: Float = 0f
) {
    fun toRGB(): Color {
        val h = (hue.turns * 6).toInt()
        val f = hue.turns * 6 - h
        val p = value * (1 - saturation)
        val q = value * (1 - f * saturation)
        val t = value * (1 - (1 - f) * saturation)

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

fun Byte.positiveRemainder(other: Byte): Byte = this.rem(other).plus(other).rem(other).toByte()
fun Short.positiveRemainder(other: Short): Short = this.rem(other).plus(other).rem(other).toShort()
fun Int.positiveRemainder(other: Int): Int = this.rem(other).plus(other).rem(other)
fun Long.positiveRemainder(other: Long): Long = this.rem(other).plus(other).rem(other)
fun Float.positiveRemainder(other: Float): Float = this.rem(other).plus(other).rem(other)
fun Double.positiveRemainder(other: Double): Double = this.rem(other).plus(other).rem(other)