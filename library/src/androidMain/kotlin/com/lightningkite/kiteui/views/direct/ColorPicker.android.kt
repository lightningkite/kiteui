package com.lightningkite.kiteui.views.direct

import android.app.AlertDialog
import android.graphics.Color as AndroidColor
import android.widget.Button as AndroidButton
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


actual class ColorPicker actual constructor(context: RContext) : RView(context) {
    override val native = AndroidButton(context.activity).apply {
        text = "Pick Color"
    }

    private val _color = Signal(Color.white)
    actual val color: MutableReactiveValue<Color> = _color

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        updateBackgroundColor()
    }

    private fun updateBackgroundColor() {
        native.setBackgroundColor(_color.value.toInt())
    }

    init {
        onRemove(_color.addListener {
            updateBackgroundColor()
        })

        native.setOnClickListener {
            if (!enabled) return@setOnClickListener

            val currentColor = _color.value
            val initialColor = AndroidColor.argb(
                (currentColor.alpha * 255).toInt(),
                (currentColor.red * 255).toInt(),
                (currentColor.green * 255).toInt(),
                (currentColor.blue * 255).toInt()
            )

            // Create a simple color picker using Android's color picker components
            val colors = arrayOf(
                AndroidColor.RED,
                AndroidColor.GREEN,
                AndroidColor.BLUE,
                AndroidColor.YELLOW,
                AndroidColor.CYAN,
                AndroidColor.MAGENTA,
                AndroidColor.WHITE,
                AndroidColor.GRAY,
                AndroidColor.BLACK
            )

            val colorNames = arrayOf(
                "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "White", "Gray", "Black"
            )

            AlertDialog.Builder(context.activity)
                .setTitle("Pick a Color")
                .setItems(colorNames) { dialog, which ->
                    val selectedColor = colors[which]
                    _color.value = Color(
                        alpha = AndroidColor.alpha(selectedColor) / 255f,
                        red = AndroidColor.red(selectedColor) / 255f,
                        green = AndroidColor.green(selectedColor) / 255f,
                        blue = AndroidColor.blue(selectedColor) / 255f
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
