package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.ClickThroughSeparator
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.awt.Color as AwtColor
import java.awt.Dimension as AwtDimension
import javax.swing.BoxLayout
import javax.swing.JSeparator
import javax.swing.SwingConstants

actual typealias Separator = SeparatorImpl

class SeparatorImpl(context: RContext) : RView(context) {
    // Use ClickThroughSeparator so it doesn't block clicks from parent Link/Button
    override val native = ClickThroughSeparator().apply {
        // Set minimum sizes to ensure separator is visible
        minimumSize = AwtDimension(1, 1)
        preferredSize = AwtDimension(1, 1)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)

        // Apply separator color from theme
        val color = theme.theme.separator.closestColor()
        val awtColor = AwtColor(
            color.red,
            color.green,
            color.blue,
            color.alpha
        )
        native.foreground = awtColor
        native.background = awtColor

        // Determine orientation based on parent container
        // JSeparator defaults to horizontal, but we can change it based on parent layout
        val parent = native.parent
        if (parent != null) {
            val layout = parent.layout
            if (layout is BoxLayout) {
                // If parent is a vertical layout (column), separator should be horizontal
                // If parent is a horizontal layout (row), separator should be vertical
                val newOrientation = if (layout.axis == BoxLayout.Y_AXIS) {
                    SwingConstants.HORIZONTAL
                } else {
                    SwingConstants.VERTICAL
                }

                if (native.orientation != newOrientation) {
                    native.orientation = newOrientation

                    // Update preferred size based on orientation
                    val thickness = theme.theme.outlineWidth.value.coerceAtLeast(1.0).toInt()
                    native.preferredSize = if (newOrientation == SwingConstants.HORIZONTAL) {
                        AwtDimension(Int.MAX_VALUE, thickness)
                    } else {
                        AwtDimension(thickness, Int.MAX_VALUE)
                    }
                }
            }
        }
    }
}
