package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import javax.swing.JProgressBar

actual class CircularProgress actual constructor(context: RContext) : RView(context) {
    override val native = JProgressBar()

    actual var ratio: Float = 0f
        set(value) {
            field = value
            native.value = (value * 100).toInt()
        }

    init {
        // TODO: For now using indeterminate JProgressBar as placeholder
        // In the future, implement custom painting for circular progress indicator
        native.isIndeterminate = true
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color for the progress indicator
        val foregroundColor = t.foreground.closestColor()
        native.foreground = foregroundColor.toAwt()

        // Apply background color
        val backgroundColor = t.background.closestColor()
        native.background = backgroundColor.toAwt()
    }
}
