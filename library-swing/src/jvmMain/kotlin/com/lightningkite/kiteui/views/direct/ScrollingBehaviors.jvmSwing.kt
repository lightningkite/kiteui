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
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.Toolkit
import java.awt.event.MouseWheelEvent
import java.lang.ref.WeakReference
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

/**
 * Global handler for mouse wheel events that forwards them to the appropriate ScrollView.
 * This is needed because BasicScrollPaneUI doesn't handle wheel events when scroll bars
 * are hidden (policy NEVER), and we want scrolling to work with hidden scroll bars.
 */
object ScrollWheelHandler {
    private val scrollViews = mutableListOf<WeakReference<ScrollView>>()
    private var initialized = false

    fun register(scrollView: ScrollView) {
        // Clean up dead references
        scrollViews.removeAll { it.get() == null }
        scrollViews.add(WeakReference(scrollView))

        if (!initialized) {
            initialized = true
            Toolkit.getDefaultToolkit().addAWTEventListener({ event ->
                if (event is MouseWheelEvent) {
                    handleWheelEvent(event)
                }
            }, AWTEvent.MOUSE_WHEEL_EVENT_MASK)
        }
    }

    private fun handleWheelEvent(e: MouseWheelEvent) {
        // Find the scroll view that contains this component
        val component = e.component ?: return

        // Walk up the component tree to find a JScrollPane
        var current: Component? = component
        while (current != null) {
            if (current is JScrollPane) {
                // Find the ScrollView that owns this JScrollPane
                val scrollView = scrollViews.mapNotNull { it.get() }.find { it.native === current }
                if (scrollView != null && scrollView.handleWheelEvent(e)) {
                    return
                }
            }
            current = current.parent
        }
    }
}

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

    private var smoothScrollTimer: javax.swing.Timer? = null

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        SwingUtilities.invokeLater {
            // Cancel any existing animation
            smoothScrollTimer?.stop()
            smoothScrollTimer = null

            val targetX = left.toInt()
            val targetY = top.toInt()

            if (animated) {
                // Smooth scroll implementation using easing
                val startX = scrollPane.viewport.viewPosition.x
                val startY = scrollPane.viewport.viewPosition.y
                val startTime = System.currentTimeMillis()
                val duration = 300L // 300ms duration for smooth feel

                smoothScrollTimer = javax.swing.Timer(16) { // ~60fps
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toDouble() / duration).coerceIn(0.0, 1.0)

                    // Ease-out cubic function for natural deceleration
                    val eased = 1 - Math.pow(1 - progress, 3.0)

                    val currentX = startX + ((targetX - startX) * eased).toInt()
                    val currentY = startY + ((targetY - startY) * eased).toInt()

                    scrollPane.viewport.viewPosition = Point(currentX, currentY)

                    if (progress >= 1.0) {
                        smoothScrollTimer?.stop()
                        smoothScrollTimer = null
                        // Ensure we land exactly on target
                        scrollPane.viewport.viewPosition = Point(targetX, targetY)
                    }
                }.apply { start() }
            } else {
                scrollPane.viewport.viewPosition = Point(targetX, targetY)
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

class ScrollView(context: RContext, private val scrollsHorizontally: Boolean, private val scrollsVertically: Boolean) : RView(context) {
    override val native = JScrollPane().apply {
        horizontalScrollBarPolicy = if (scrollsHorizontally) {
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        } else {
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        verticalScrollBarPolicy = if (scrollsVertically) {
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        } else {
            JScrollPane.VERTICAL_SCROLLBAR_NEVER
        }
        // Make viewport transparent by default - background will be set by applyTheme
        viewport.isOpaque = false

        // Custom wheel listener that works even when scrollbars are hidden.
        // BasicScrollPaneUI's default handler checks scrollbar.isVisible() which fails
        // when policy is NEVER, so we implement our own scrolling logic.
        addMouseWheelListener { e ->
            if (!isWheelScrollingEnabled) return@addMouseWheelListener

            val scrollBar = if (scrollsVertically) verticalScrollBar else horizontalScrollBar
            if (scrollBar == null) return@addMouseWheelListener

            // Calculate scroll amount (similar to BasicScrollPaneUI logic)
            val scrollAmount = e.scrollAmount * e.wheelRotation * scrollBar.unitIncrement
            val newValue = (scrollBar.value + scrollAmount).coerceIn(scrollBar.minimum, scrollBar.maximum - scrollBar.visibleAmount)

            if (newValue != scrollBar.value) {
                scrollBar.value = newValue
                e.consume()
            }
        }
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

    init {
        // Register this scroll view for global wheel event handling
        ScrollWheelHandler.register(this)
    }

    override fun internalAddChild(index: Int, view: RView) {
        // JScrollPane can only have one viewport view
        // If adding first child, set it as the viewport view
        if (index == 0) {
            SwingUtilities.invokeLater {
                // If the child is a Scrollable panel, tell it which direction we're scrolling
                val childNative = view.native
                if (childNative is ScrollableLinearPanel) {
                    // If horizontal scrolling is enabled, the content should grow horizontally
                    // If only vertical scrolling is enabled, the content should grow vertically
                    childNative.isVerticalScrolling = !scrollsHorizontally
                } else if (childNative is com.lightningkite.kiteui.views.ScrollableMouseTransparentPanel) {
                    // Same for ScrollableMouseTransparentPanel (used by ProgrammaticLayout)
                    childNative.isVerticalScrolling = !scrollsHorizontally
                }
                native.setViewportView(childNative)
            }
        } else {
            // JScrollPane only supports one child in the viewport
            throw UnsupportedOperationException("ScrollView can only have one child. Wrap multiple children in a container first.")
        }
    }

    fun handleWheelEvent(e: java.awt.event.MouseWheelEvent): Boolean {
        if (!native.isWheelScrollingEnabled) return false

        val scrollBar = if (scrollsVertically) native.verticalScrollBar else native.horizontalScrollBar
        if (scrollBar == null) return false

        // Calculate scroll amount
        val scrollAmount = e.scrollAmount * e.wheelRotation * scrollBar.unitIncrement
        val newValue = (scrollBar.value + scrollAmount).coerceIn(scrollBar.minimum, scrollBar.maximum - scrollBar.visibleAmount)

        if (newValue != scrollBar.value) {
            scrollBar.value = newValue
            return true
        }
        return false
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
