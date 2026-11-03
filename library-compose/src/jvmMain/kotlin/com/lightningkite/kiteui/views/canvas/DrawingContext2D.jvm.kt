package com.lightningkite.kiteui.views.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Gradient
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Shader

// HTML canvas blurs shadows with a standard deviation of half the shadowBlur.
internal actual fun Paint.setShadow(color: Color, offset: Offset, blur: Float) {
    skiaPaint.imageFilter = ImageFilter.makeDropShadow(offset.x, offset.y, blur / 2, blur / 2, color.toArgb())
}

// Interpolated premultiplied, like Compose's own gradients that draw the canvas's linear ones.
internal actual fun canvasRadialGradient(stops: List<Pair<Float, Color>>, focus: Offset, center: Offset, radius: Float): Brush {
    val gradient = Gradient(
        Gradient.Colors(
            colors = stops.map { (_, color) -> Color4f(color.red, color.green, color.blue, color.alpha) }.toTypedArray(),
            positions = stops.map { (ratio, _) -> ratio }.toFloatArray(),
            tileMode = FilterTileMode.CLAMP,
        ),
        Gradient.Interpolation(inPremul = Gradient.Interpolation.InPremul.YES),
    )
    return ShaderBrush(Shader.makeTwoPointConicalGradient(focus.x, focus.y, 0f, center.x, center.y, radius, gradient).asComposeShader())
}
