package com.lightningkite.kiteui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

fun ImageScaleType.toCompose(): ContentScale {
    return when (this) {
        ImageScaleType.Fit -> ContentScale.Fit
        ImageScaleType.Crop -> ContentScale.Crop
        ImageScaleType.Stretch -> ContentScale.FillBounds
        ImageScaleType.NoScale -> ContentScale.None
    }
}

@Composable
fun com.lightningkite.kiteui.models.ImageVector.toPainter(): androidx.compose.ui.graphics.painter.Painter {
    val imageVector = remember(this) {
        ImageVector.Builder(
            defaultWidth = width.value.dp,
            defaultHeight = height.value.dp,
            viewportWidth = viewBoxWidth.toFloat(),
            viewportHeight = viewBoxHeight.toFloat()
        ).apply {
            for (path in paths) {
                addPath(
                    pathData = PathParser().parsePathString(path.path).toNodes(),
                    fill = path.fillColor?.toBrush(),
                    stroke = path.strokeColor?.let { SolidColor(it.toComposeColor()) },
                    strokeLineWidth = path.strokeWidth?.toFloat() ?: 0f,
                )
            }
        }.build()
    }
    return rememberVectorPainter(imageVector)
}

fun Paint.toBrush(): androidx.compose.ui.graphics.Brush? {
    return when (this) {
        is com.lightningkite.kiteui.models.Color -> SolidColor(this.toComposeColor())
        is LinearGradient -> {
            val angleRad = angle.radians.toFloat()
            Brush.linearGradient(
                colorStops = stops.map { it.ratio to it.color.toComposeColor() }.toTypedArray(),
                start = Offset(0f, 0f),
                end = Offset(cos(angleRad), sin(angleRad))
            )
        }
        is RadialGradient -> Brush.radialGradient(
            colorStops = stops.map { it.ratio to it.color.toComposeColor() }.toTypedArray(),
        )
        else -> null
    }
}

fun com.lightningkite.kiteui.models.Color.toComposeColor(): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(red, green, blue, alpha)
}