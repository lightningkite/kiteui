package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.px
import java.awt.Dimension as AwtDimension
import javax.swing.JProgressBar
import kotlin.math.roundToInt

actual typealias ActivityIndicator = ActivityIndicatorImpl

class ActivityIndicatorImpl(context: RContext) : RView(context) {
    override val native = JProgressBar().apply {
        isIndeterminate = true
        // Set appropriate default size for indeterminate progress
        val defaultSize = 24
        minimumSize = AwtDimension(defaultSize, defaultSize)
        preferredSize = AwtDimension(defaultSize, defaultSize)
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

        // Update size constraints based on theme
        val size = (1.rem.px).roundToInt()
        val dimension = AwtDimension(size, size)
        native.minimumSize = dimension
        native.preferredSize = dimension
    }
}
