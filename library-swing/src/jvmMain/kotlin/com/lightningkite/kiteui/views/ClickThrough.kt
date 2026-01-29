package com.lightningkite.kiteui.views

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JSeparator

/**
 * Click-through support for Swing components.
 *
 * In Swing, mouse events go to the topmost component and don't bubble up.
 * For clickable containers (Button, Link, ExternalLink), we use JLayeredPane
 * with a transparent JButton in a higher layer to receive all mouse events.
 *
 * The click-through components below use contains()=false to make themselves
 * mouse-transparent, allowing events to pass through to the underlying layer.
 */

/**
 * A JPanel that doesn't intercept mouse events itself but allows children to receive them.
 * Use this for container components (Frame, SwapView) that should not handle clicks directly.
 *
 * Unlike ClickThroughPanel which uses contains()=false, this panel lets Swing recurse
 * into children normally. It just doesn't add any mouse listeners of its own.
 *
 * When the client property "kiteui.ignoreInteraction" is set to true, this panel becomes
 * completely mouse-transparent (contains() returns false), allowing all events to pass through
 * to components underneath in the z-order.
 */
open class MouseTransparentPanel : JPanel() {
    // No mouse listeners - let children handle events directly

    override fun contains(x: Int, y: Int): Boolean {
        // When ignoreInteraction is set, become completely transparent to mouse events
        val ignoreInteraction = getClientProperty("kiteui.ignoreInteraction") as? Boolean ?: false
        if (ignoreInteraction) {
            return false
        }
        // Otherwise, use default behavior - let Swing recurse into children
        return super.contains(x, y)
    }
}

/**
 * A MouseTransparentPanel that implements Scrollable for proper behavior inside JScrollPane.
 * Used by ProgrammaticLayout to enable scrolling of programmatically laid out content.
 */
open class ScrollableMouseTransparentPanel : MouseTransparentPanel(), javax.swing.Scrollable {
    /**
     * Whether the primary scroll direction is vertical.
     * When true (vertical scrolling): track viewport width, allow height to grow
     * When false (horizontal scrolling): track viewport height, allow width to grow
     */
    var isVerticalScrolling: Boolean = true
        set(value) {
            field = value
            revalidate()
        }

    override fun getScrollableTracksViewportWidth(): Boolean = isVerticalScrolling  // For vertical scroll, constrain width
    override fun getScrollableTracksViewportHeight(): Boolean = !isVerticalScrolling  // For horizontal scroll, constrain height

    override fun getPreferredScrollableViewportSize(): java.awt.Dimension = preferredSize

    override fun getScrollableUnitIncrement(visibleRect: java.awt.Rectangle?, orientation: Int, direction: Int): Int = 20

    override fun getScrollableBlockIncrement(visibleRect: java.awt.Rectangle?, orientation: Int, direction: Int): Int {
        return if (orientation == javax.swing.SwingConstants.VERTICAL) {
            visibleRect?.height ?: 100
        } else {
            visibleRect?.width ?: 100
        }
    }
}

/**
 * A JLabel that doesn't intercept mouse events.
 * Use for text display inside clickable containers.
 */
open class ClickThroughLabel : JLabel() {
    override fun contains(x: Int, y: Int): Boolean = false
}

/**
 * A JSeparator that doesn't intercept mouse events.
 */
open class ClickThroughSeparator : JSeparator() {
    override fun contains(x: Int, y: Int): Boolean = false
}

/**
 * A JProgressBar that doesn't intercept mouse events.
 */
open class ClickThroughProgressBar : JProgressBar {
    constructor() : super()
    constructor(min: Int, max: Int) : super(min, max)

    override fun contains(x: Int, y: Int): Boolean = false
}

/**
 * A JPanel that doesn't intercept mouse events.
 * Use for non-interactive leaf components like Space.
 */
open class ClickThroughPanel : JPanel() {
    override fun contains(x: Int, y: Int): Boolean = false
}
