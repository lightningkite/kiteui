package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import javax.swing.JProgressBar

actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    override val native = JProgressBar(0, 100).apply {
        // Set progress bar to be horizontal (default) and determinate
        isIndeterminate = false
    }

    init {
        // Ensure the progress bar displays correctly with initial value
        native.value = 0
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color (the progress bar fill)
        val foregroundColor = t.foreground.closestColor()
        native.foreground = foregroundColor.toAwt()

        // Apply background color (the unfilled portion)
        val backgroundColor = t.background.closestColor()
        native.background = backgroundColor.toAwt()
    }

    actual var ratio: Float
        get() = native.value / 100.0f
        set(value) {
            // Clamp value between 0.0 and 1.0, then convert to 0-100 range
            val clampedValue = value.coerceIn(0f, 1f)
            native.value = (clampedValue * 100f).toInt()
        }
}
