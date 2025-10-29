package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


actual class ColorPicker actual constructor(context: RContext) : RView(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "input"
        native.attributes.type = "color"
        native.classes.add("colorPicker")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
    }

    actual val color: MutableReactiveValue<Color> = native.vprop(
        "input",
        get = {
            // Convert hex color (#RRGGBB) to Color object
            val hexValue = attributes.valueString ?: "#FFFFFF"
            try {
                val hex = hexValue.removePrefix("#")
                val r = hex.substring(0, 2).toInt(16) / 255f
                val g = hex.substring(2, 4).toInt(16) / 255f
                val b = hex.substring(4, 6).toInt(16) / 255f
                Color(alpha = 1f, red = r, green = g, blue = b)
            } catch (e: Exception) {
                Color.white
            }
        },
        set = { value ->
            // Convert Color object to hex color (#RRGGBB)
            val r = (value.red * 255).toInt().coerceIn(0, 255)
            val g = (value.green * 255).toInt().coerceIn(0, 255)
            val b = (value.blue * 255).toInt().coerceIn(0, 255)
            attributes.valueString = "#${r.toString(16).padStart(2, '0').uppercase()}${g.toString(16).padStart(2, '0').uppercase()}${b.toString(16).padStart(2, '0').uppercase()}"
        }
    )

    actual var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
