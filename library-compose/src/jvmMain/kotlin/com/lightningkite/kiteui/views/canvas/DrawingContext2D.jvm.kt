package com.lightningkite.kiteui.views.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.ImageFilter

// HTML canvas blurs shadows with a standard deviation of half the shadowBlur.
internal actual fun Paint.setShadow(color: Color, offset: Offset, blur: Float) {
    skiaPaint.imageFilter = ImageFilter.makeDropShadow(offset.x, offset.y, blur / 2, blur / 2, color.toArgb())
}
