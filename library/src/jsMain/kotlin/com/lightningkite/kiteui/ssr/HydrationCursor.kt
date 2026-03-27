package com.lightningkite.kiteui.ssr

import org.w3c.dom.Element

/**
 * Tracks position within a DOM subtree during hydration.
 * Provides children one at a time for matching with view tree.
 */
class HydrationCursor(val element: Element) {
    private var childIndex = 0

    /**
     * Get next child element for hydration, or null if no more children.
     */
    fun nextChild(): Element? {
        val child = element.children.item(childIndex)
        if (child != null) childIndex++
        return child
    }

    /**
     * Create a sub-cursor for the given element's children.
     */
    fun forElement(element: Element) = HydrationCursor(element)

    /**
     * Check if all children were consumed (for mismatch detection).
     */
    fun hasRemaining(): Boolean = childIndex < element.childElementCount

    /**
     * Get count of remaining unconsumed children.
     */
    fun remainingCount(): Int = element.childElementCount - childIndex
}
