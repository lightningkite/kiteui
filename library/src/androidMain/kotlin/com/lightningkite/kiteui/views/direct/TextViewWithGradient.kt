package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Shader
import androidx.appcompat.widget.AppCompatTextView
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Paint

class TextViewWithGradient(context: Context): AppCompatTextView(context) {

    var kuiPaintForeground: Paint = Color.black
        set(f) {
            field = f
            when (f) {
                is Color -> {
                    setTextColor(f.colorInt())
                }
                is LinearGradient -> {
                    createAndSetBoundedGradientShader(f)
                }
                else -> {
                    setTextColor(f.colorInt())
                }
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        (kuiPaintForeground as? LinearGradient)?.let(::createAndSetBoundedGradientShader)
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        paint.shader = null
    }

    private fun createAndSetBoundedGradientShader(gradient: LinearGradient) {
        if (width <= 0 || height <= 0) return
        setTextColor(android.graphics.Color.BLACK)

        val x: Float
        val y: Float
        val shiftX: Float
        val shiftY: Float
        if (gradient.angle.tan() < (height / width)) {
            x = width.toFloat()
            y = gradient.angle.sin() * (x / gradient.angle.cos())
            shiftX = 0f
            shiftY = (height - y) / 2
        } else {
            y = height.toFloat()
            x = gradient.angle.cos() * (y / gradient.angle.sin())
            shiftX = (width - x) / 2
            shiftY = 0f
        }

        paint.shader = android.graphics.LinearGradient(
            0f + shiftX,
            height - shiftY,
            x + shiftX,
            height - y - shiftY,
            gradient.stops.map { it.color.colorInt() }.toIntArray(),
            gradient.stops.map { it.ratio }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
    }
}