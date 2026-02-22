package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.views.MouseTransparentPanel
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.awt.LayoutManager
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

// Frame is just a basic container
actual typealias Frame = FrameImpl

class FrameImpl(context: RContext) : RView(context) {
    override val native = MouseTransparentPanel().apply {
        layout = FrameLayoutManager()
    }
}

/**
 * A simple layout manager that behaves like Android's FrameLayout.
 * All children are laid out to fill the parent's bounds, stacked on top of each other.
 */
private class FrameLayoutManager : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
        // No-op
    }

    override fun removeLayoutComponent(comp: Component?) {
        // No-op
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        // Calculate the maximum preferred size among all children
        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                val childSize = child.preferredSize
                maxWidth = maxOf(maxWidth, childSize.width)
                maxHeight = maxOf(maxHeight, childSize.height)
            }
        }

        val insets = parent.insets
        return Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        // Calculate the maximum minimum size among all children
        var maxWidth = 0
        var maxHeight = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                val childSize = child.minimumSize
                maxWidth = maxOf(maxWidth, childSize.width)
                maxHeight = maxOf(maxHeight, childSize.height)
            }
        }

        val insets = parent.insets
        return Dimension(
            maxWidth + insets.left + insets.right,
            maxHeight + insets.top + insets.bottom
        )
    }

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val availableWidth = parent.width - insets.left - insets.right
        val availableHeight = parent.height - insets.top - insets.bottom

        // Lay out all children respecting their alignment
        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (!child.isVisible) continue

            // Get alignment settings
            val horizontalAlign = if (child is JComponent) child.horizontalAlign else Align.Stretch
            val verticalAlign = if (child is JComponent) child.verticalAlign else Align.Stretch

            // Calculate child size based on alignment
            val prefSize = child.preferredSize
            val childWidth = when (horizontalAlign) {
                Align.Stretch -> availableWidth
                else -> prefSize.width.coerceAtMost(availableWidth)
            }
            val childHeight = when (verticalAlign) {
                Align.Stretch -> availableHeight
                else -> prefSize.height.coerceAtMost(availableHeight)
            }

            // Calculate position based on alignment
            val x = when (horizontalAlign) {
                Align.Start -> insets.left
                Align.Center -> insets.left + (availableWidth - childWidth) / 2
                Align.End -> insets.left + (availableWidth - childWidth)
                Align.Stretch -> insets.left
            }
            val y = when (verticalAlign) {
                Align.Start -> insets.top
                Align.Center -> insets.top + (availableHeight - childHeight) / 2
                Align.End -> insets.top + (availableHeight - childHeight)
                Align.Stretch -> insets.top
            }

            child.setBounds(x, y, childWidth, childHeight)
        }
    }
}
