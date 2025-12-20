package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

actual class Link actual constructor(context: RContext) : RView(context) {
    override val native = JPanel().apply {
        layout = LinkLayoutManager()
        isOpaque = false
    }

    actual var enabled: Boolean = true
        set(value) {
            field = value
            refreshTheming()
        }

    actual var to: (() -> Page)? = null
        set(value) {
            field = value
            updateClickHandler()
        }

    actual var onNavigator: PageNavigator = mainPageNavigator

    actual var newTab: Boolean = false

    actual var resetsStack: Boolean = false

    private var onClick: (suspend () -> Unit)? = null
    actual fun onClick(action: suspend () -> Unit) {
        onClick = action
    }

    private var onNavigate: (suspend () -> Unit)? = null
    actual fun onNavigate(action: suspend () -> Unit) {
        onNavigate = action
    }

    private fun updateClickHandler() {
        // Remove existing mouse listeners
        native.mouseListeners.forEach { native.removeMouseListener(it) }

        // Add mouse listener for click handling
        native.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (enabled && e.button == MouseEvent.BUTTON1) {
                    onClick?.let { launch { it() } }
                    to?.invoke()?.let { page ->
                        launch {
                            onNavigate?.invoke()
                            if (resetsStack) {
                                onNavigator.reset(page)
                            } else {
                                onNavigator.navigate(page)
                            }
                        }
                    }
                }
            }
        })
    }

    init {
        updateClickHandler()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}

/**
 * Simple layout manager for Link that lays out children in a stack (like FrameLayout).
 * All children fill the parent's bounds.
 */
private class LinkLayoutManager : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): Dimension {
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
        val width = parent.width - insets.left - insets.right
        val height = parent.height - insets.top - insets.bottom

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (child.isVisible) {
                child.setBounds(insets.left, insets.top, width, height)
            }
        }
    }
}
