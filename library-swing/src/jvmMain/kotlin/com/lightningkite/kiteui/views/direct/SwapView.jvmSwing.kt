package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.CalculationContext
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager
import java.awt.event.ActionListener
import javax.swing.JPanel
import javax.swing.Timer

actual class SwapView actual constructor(context: RContext) : RView(context) {
    override val native = JPanel().apply {
        layout = SwapLayoutManager()
    }

    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit?) {
        val oldView = this.children.firstOrNull()
        var newViewHolder: RView? = null

        // Create a ViewWriter to capture the new view
        val writer = object : ViewWriter(), CalculationContext by this {
            override val representsView: RView? = this@SwapView
            override val context: RContext
                get() = this@SwapView.context

            override fun willAddChild(view: RView) {
                view.parent = this@SwapView
            }

            override fun addChild(view: RView) {
                newViewHolder = view
            }
        }

        // Create the new view
        writer.createNewView()
        val newView = newViewHolder

        // Handle the swap with optional animation
        when {
            transition == ScreenTransition.Fade && oldView != null && newView != null -> {
                // Perform fade transition
                animateFadeTransition(oldView, newView)
            }
            else -> {
                // No animation, just swap immediately
                oldView?.let { removeChild(it) }
                newView?.let { addChild(it) }
                native.revalidate()
                native.repaint()
            }
        }
    }

    private fun animateFadeTransition(oldView: RView, newView: RView) {
        val duration = theme.transitionDuration.inWholeMilliseconds.toInt()
        val fps = 60
        val frameDelay = 1000 / fps
        val totalFrames = duration / frameDelay
        var currentFrame = 0

        // Add the new view (initially invisible)
        addChild(newView)
        newView.native.isVisible = false

        // Create a timer for animation
        val timer = Timer(frameDelay, null)
        val listener = ActionListener {
            currentFrame++
            val progress = currentFrame.toFloat() / totalFrames.toFloat()

            if (currentFrame >= totalFrames) {
                // Animation complete
                removeChild(oldView)
                newView.native.isVisible = true
                timer.stop()
                native.revalidate()
                native.repaint()
            } else {
                // Update alpha values
                setComponentAlpha(oldView.native, 1f - progress)
                newView.native.isVisible = true
                setComponentAlpha(newView.native, progress)
                native.repaint()
            }
        }

        timer.addActionListener(listener)
        timer.start()
    }

    private fun setComponentAlpha(component: Component, alpha: Float) {
        // Store alpha value in client property for use during painting
        if (component is javax.swing.JComponent) {
            component.putClientProperty("kiteui.alpha", alpha)
        }
        // Note: Actual alpha rendering would require overriding paintComponent
        // For now, we'll use visibility as a simpler approach
        if (alpha <= 0.01f) {
            component.isVisible = false
        } else if (alpha >= 0.99f) {
            component.isVisible = true
        }
    }
}

/**
 * Layout manager for SwapView that makes each child fill the entire container
 */
private class SwapLayoutManager : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {
        // No-op
    }

    override fun removeLayoutComponent(comp: Component?) {
        // No-op
    }

    override fun preferredLayoutSize(parent: Container): Dimension {
        // Return the maximum preferred size among all children
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
        // Return the maximum minimum size among all children
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

        // Lay out all children to fill the entire space
        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible || child is javax.swing.JComponent && child.getClientProperty("kiteui.alpha") != null) {
                child.setBounds(insets.left, insets.top, availableWidth, availableHeight)
            }
        }
    }
}
