package com.lightningkite.kiteui.views

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.boundingRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Paint as SkiaPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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

// As the web's ::placeholder: the text color, at 30% opacity.
internal val Theme.hintColor: ComposeColor get() = foreground.closestColor().let { it.withAlpha(it.alpha * 0.3f) }.toCompose()

public fun Paint.toBrush(): Brush = when (this) {
    is Color -> SolidColor(toCompose())
    // Element backgrounds fade between the two (see NativeElement.Render), as on the web; elsewhere it's the base.
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

// The web's box-shadow for an elevation (KiteUiCss's toBoxShadow), which like any box-shadow is drawn only outside the
// element.
internal fun DrawScope.drawElevationShadow(outline: Outline, elevation: Float) {
    if (elevation <= 0f) return
    val box = when (outline) {
        is Outline.Rectangle -> RoundRect(outline.rect)
        is Outline.Rounded -> outline.roundRect
        is Outline.Generic -> error("Only rounded rectangles cast shadows")
    }
    fun shadow(offsetY: Float, blur: Float, spread: Float, alpha: Float) {
        val rect = box.boundingRect.inflate(spread).translate(0f, offsetY)
        // As in CSS, spread grows the corners' radii with the box.
        val radii = listOf(box.topLeftCornerRadius, box.topRightCornerRadius, box.bottomRightCornerRadius, box.bottomLeftCornerRadius)
            .flatMap { listOf(maxOf(0f, it.x + spread), maxOf(0f, it.y + spread)) }
        // A rounded rectangle, not a path: Skia caches its blur, which makes scrolling past shadows several times faster.
        val shadowBox = RRect.makeComplexLTRB(rect.left, rect.top, rect.right, rect.bottom, radii.toFloatArray())
        val paint = SkiaPaint().apply {
            color = ComposeColor.Black.copy(alpha = alpha).toArgb()
            // CSS blurs with a standard deviation of half the blur radius.
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur / 2)
        }
        drawIntoCanvas { it.nativeCanvas.drawRRect(shadowBox, paint) }
    }
    clipPath(Path().apply { addOutline(outline) }, ClipOp.Difference) {
        shadow(offsetY = elevation * 1.5f, blur = elevation / 2, spread = -elevation, alpha = 0.2f)
        shadow(offsetY = elevation, blur = elevation, spread = 0f, alpha = 0.14f)
        shadow(offsetY = elevation / 2, blur = elevation * 2.5f, spread = 0f, alpha = 0.12f)
    }
}

public expect fun Font.toFontFamily(): FontFamily

// Compose's TextStyle has no text-transform, so allCaps changes the text itself wherever it's shown.  Character by
// character, keeping the length, so a field's offsets still line up.
internal fun FontAndStyle.cased(text: String): String = if (allCaps) text.uppercaseKeepingLength() else text

// A field shows its value cased while the value keeps what was typed, as with the web's text-transform.
internal val FontAndStyle.casing: VisualTransformation get() = if (allCaps) AllCaps else VisualTransformation.None

private val AllCaps = VisualTransformation { TransformedText(AnnotatedString(it.text.uppercaseKeepingLength()), OffsetMapping.Identity) }

private fun String.uppercaseKeepingLength() = String(CharArray(length) { this[it].uppercaseChar() })

public fun FontAndStyle.toTextStyle(color: ComposeColor, align: Align = this.align): TextStyle = TextStyle(
    color = color,
    fontFamily = font.toFontFamily(),
    fontWeight = FontWeight(weight),
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.value.sp,
    lineHeight = lineSpacingMultiplier.em,
    // Compose trims the leading above the first line and below the last by default; CSS keeps it, centering each line in its line-height.
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
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
