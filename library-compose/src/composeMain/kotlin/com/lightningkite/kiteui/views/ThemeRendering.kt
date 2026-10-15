package com.lightningkite.kiteui.views

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lightningkite.kiteui.models.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.Color as ComposeColor

public fun Color.toCompose(): ComposeColor = ComposeColor(red, green, blue, alpha)

internal val Theme.hintColor: ComposeColor get() = foreground.closestColor().withAlpha(0.5f).toCompose()

public fun Paint.toBrush(): Brush = when (this) {
    is Color -> SolidColor(toCompose())
    // TODO: animate between base and alternate; animations are out of scope for now.
    is FadingColor -> SolidColor(base.toCompose())
    is LinearGradient -> AngledLinearGradient(this)
    is RadialGradient -> Brush.radialGradient(*stops.map { it.ratio to it.color.toCompose() }.toTypedArray())
}

// Compose's linearGradient takes absolute endpoints; KiteUI gives an angle relative to the box, like CSS.
private class AngledLinearGradient(val gradient: LinearGradient) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = gradient.angle.radians
        val direction = Offset(cos(radians), sin(radians))
        // Half the gradient line's length, chosen (as CSS does) so the end colors land exactly on the corners.
        val halfLength = abs(size.width / 2 * direction.x) + abs(size.height / 2 * direction.y)
        val center = Offset(size.width / 2, size.height / 2)
        return LinearGradientShader(
            from = center - direction * halfLength,
            to = center + direction * halfLength,
            colors = gradient.stops.map { it.color.toCompose() },
            colorStops = gradient.stops.map { it.ratio },
        )
    }

    override fun equals(other: Any?): Boolean = other is AngledLinearGradient && other.gradient == gradient
    override fun hashCode(): Int = gradient.hashCode()
}

public fun Theme.shape(parentSpacing: Dimension): Shape = when (val c = cornerRadii) {
    is CornerRadii.AdaptiveToSpacing -> RoundedCornerShape(minOf(parentSpacing, c.value).toDp())
    is CornerRadii.Fixed -> RoundedCornerShape(c.value.toDp())
    is CornerRadii.RatioOfSize -> RoundedCornerShape(percent = (c.ratio.coerceAtMost(0.5f) * 100).toInt())
    is CornerRadii.RatioOfSpacing -> RoundedCornerShape((parentSpacing * c.value).toDp())
    is CornerRadii.PerCorner -> {
        val r = c.value.toDp()
        RoundedCornerShape(
            topStart = if (c.topLeft) r else 0.dp,
            topEnd = if (c.topRight) r else 0.dp,
            bottomEnd = if (c.bottomRight) r else 0.dp,
            bottomStart = if (c.bottomLeft) r else 0.dp,
        )
    }
}

public expect fun Font.toFontFamily(): FontFamily

public fun FontAndStyle.toTextStyle(color: ComposeColor, align: Align = this.align): TextStyle = TextStyle(
    color = color,
    fontFamily = font.toFontFamily(),
    fontWeight = FontWeight(weight),
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.value.sp,
    lineHeight = lineSpacingMultiplier.em,
    letterSpacing = additionalLetterSpacing.value.sp,
    textDecoration = TextDecoration.combine(
        listOfNotNull(
            TextDecoration.Underline.takeIf { underline },
            TextDecoration.LineThrough.takeIf { strikethrough },
        )
    ),
    textAlign = when (align) {
        Align.Start -> TextAlign.Start
        Align.Center -> TextAlign.Center
        Align.End -> TextAlign.End
        Align.Stretch -> TextAlign.Justify
    },
)
