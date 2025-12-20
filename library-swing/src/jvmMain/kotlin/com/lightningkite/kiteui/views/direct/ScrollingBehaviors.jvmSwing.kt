package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class ScrollingBehaviorsImpl(val scrollPane: JScrollPane) : ScrollingBehaviors {
    override val horizontal: Boolean = scrollPane.horizontalScrollBarPolicy != JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    override val vertical: Boolean = scrollPane.verticalScrollBarPolicy != JScrollPane.VERTICAL_SCROLLBAR_NEVER

    override var showScrollBars: Boolean = true
        set(value) {
            field = value
            SwingUtilities.invokeLater {
                scrollPane.horizontalScrollBarPolicy = if (!value) {
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                } else if (horizontal) {
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                } else {
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                }
                scrollPane.verticalScrollBarPolicy = if (!value) {
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER
                } else if (vertical) {
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                } else {
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER
                }
            }
        }

    private val _viewport: Signal<Rect> = Signal(Rect.Zero)
    override val viewport: Reactive<Rect> get() = _viewport

    private val _content: Signal<Rect> = Signal(Rect.Zero)
    override val content: Reactive<Rect> get() = _content

    private val _directlyInteractingWithScroller: Signal<Boolean> = Signal(false)
    override val directlyInteractingWithScroller: Reactive<Boolean> get() = _directlyInteractingWithScroller

    init {
        // Track viewport changes
        val adjustmentListener = AdjustmentListener { e ->
            updateViewportAndContent()

            // Track when user is actively scrolling
            if (!e.valueIsAdjusting) {
                _directlyInteractingWithScroller.value = false
            } else {
                _directlyInteractingWithScroller.value = true
            }
        }

        scrollPane.horizontalScrollBar.addAdjustmentListener(adjustmentListener)
        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentListener)

        // Track viewport size changes
        scrollPane.viewport.addChangeListener {
            updateViewportAndContent()
        }

        // Initial update
        SwingUtilities.invokeLater {
            updateViewportAndContent()
        }
    }

    private fun updateViewportAndContent() {
        val viewportView = scrollPane.viewport.view ?: return
        val viewportRect = scrollPane.viewport.viewRect
        val viewSize = viewportView.size

        // Update viewport (visible area)
        _viewport.value = Rect.fromSize(
            left = viewportRect.x.toDouble(),
            top = viewportRect.y.toDouble(),
            width = viewportRect.width.toDouble(),
            height = viewportRect.height.toDouble()
        )

        // Update content (total scrollable area)
        _content.value = Rect.fromSize(
            left = 0.0,
            top = 0.0,
            width = viewSize.width.toDouble(),
            height = viewSize.height.toDouble()
        )
    }

    override var snapToElements: Pair<Align?, Align?> = null to null
    override var scrollSnapStop: Boolean = false

    override var ignoreInteraction: Boolean = false
        set(value) {
            field = value
            SwingUtilities.invokeLater {
                scrollPane.isEnabled = !value
                scrollPane.viewport.view?.let { it.isEnabled = !value }
            }
        }

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        SwingUtilities.invokeLater {
            // Swing doesn't natively support animated scrolling, but we can implement smooth scrolling
            if (animated) {
                // Simple smooth scroll implementation
                val currentX = scrollPane.horizontalScrollBar.value
                val currentY = scrollPane.verticalScrollBar.value
                val targetX = left.toInt()
                val targetY = top.toInt()

                // For now, just jump to position
                // TODO: Implement smooth animation using Timer
                scrollPane.viewport.viewPosition = Point(targetX, targetY)
            } else {
                scrollPane.viewport.viewPosition = Point(left.toInt(), top.toInt())
            }
        }
    }

    override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        SwingUtilities.invokeLater {
            val viewportView = scrollPane.viewport.view ?: return@invokeLater
            val elementNative = element.native

            // Get element bounds relative to viewport
            val elementBounds = SwingUtilities.convertRectangle(
                elementNative.parent,
                elementNative.bounds,
                viewportView
            )

            val viewportRect = scrollPane.viewport.viewRect

            // Calculate target scroll position based on alignment
            val targetX = when (horizontal) {
                Align.Start -> elementBounds.x
                Align.Center -> elementBounds.x - (viewportRect.width - elementBounds.width) / 2
                Align.End -> elementBounds.x - (viewportRect.width - elementBounds.width)
                Align.Stretch -> elementBounds.x // Same as Start
            }.coerceIn(0, maxOf(0, viewportView.width - viewportRect.width))

            val targetY = when (vertical) {
                Align.Start -> elementBounds.y
                Align.Center -> elementBounds.y - (viewportRect.height - elementBounds.height) / 2
                Align.End -> elementBounds.y - (viewportRect.height - elementBounds.height)
                Align.Stretch -> elementBounds.y // Same as Start
            }.coerceIn(0, maxOf(0, viewportView.height - viewportRect.height))

            scrollTo(targetX.toDouble(), targetY.toDouble(), animated)
        }
    }

    override fun scrollToKeepAnimations(x: Double, y: Double) {
        SwingUtilities.invokeLater {
            scrollPane.viewport.viewPosition = Point(x.toInt(), y.toInt())
        }
    }
}

class ScrollView(context: RContext, horizontal: Boolean, vertical: Boolean) : RView(context) {
    override val native = JScrollPane().apply {
        horizontalScrollBarPolicy = if (horizontal) {
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        } else {
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        verticalScrollBarPolicy = if (vertical) {
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        } else {
            JScrollPane.VERTICAL_SCROLLBAR_NEVER
        }
        // Make viewport transparent by default - background will be set by applyTheme
        viewport.isOpaque = false
    }

    override fun applyTheme(theme: com.lightningkite.kiteui.models.ThemeAndBack) {
        super.applyTheme(theme)

        // Apply background to the viewport as well
        if (theme.drawBackground) {
            val backgroundColor = theme.theme.background.closestColor()
            native.viewport.background = backgroundColor.toAwt()
            native.viewport.isOpaque = true
        } else {
            native.viewport.isOpaque = false
        }
    }

    override fun internalAddChild(index: Int, view: RView) {
        // JScrollPane can only have one viewport view
        // If adding first child, set it as the viewport view
        if (index == 0) {
            SwingUtilities.invokeLater {
                native.setViewportView(view.native)
            }
        } else {
            // JScrollPane only supports one child in the viewport
            throw UnsupportedOperationException("ScrollView can only have one child. Wrap multiple children in a container first.")
        }
    }

    override fun internalRemoveChild(index: Int) {
        if (index == 0) {
            SwingUtilities.invokeLater {
                native.setViewportView(null)
            }
        }
    }

    override fun internalClearChildren() {
        SwingUtilities.invokeLater {
            native.setViewportView(null)
        }
    }
}
