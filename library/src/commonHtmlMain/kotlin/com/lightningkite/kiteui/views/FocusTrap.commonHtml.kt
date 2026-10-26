package com.lightningkite.kiteui.views

import com.lightningkite.reactive.context.onRemove

/**
 * Web implementation of [trapFocus].
 *
 * Siblings of this element are marked `inert` so that they cannot be focused or clicked and are
 * hidden from assistive technology. Focus is moved to the first interactive descendant, falling
 * back to this element itself (given a `tabindex` so it can receive focus). Both the `inert`
 * markers and the previously focused element are restored when this element is removed. Siblings
 * shared by stacked modals stay inert until the last of those modals is removed.
 */
public actual fun Element.trapFocus() {
    val siblings = parent?.children?.filter { it !== this }.orEmpty()
    val previouslyFocused = captureWebFocus()
    siblings.forEach {
        inertCounts[it] = (inertCounts[it] ?: 0) + 1
        it.native.setAttribute("inert", "")
    }

    // Ensure the scope itself can receive focus if it has no interactive descendants.
    native.setAttribute("tabindex", "-1")
    requestFocusOrDescendant()

    onRemove {
        siblings.forEach {
            val remaining = (inertCounts[it] ?: 1) - 1
            if (remaining > 0) {
                inertCounts[it] = remaining
            } else {
                inertCounts.remove(it)
                it.native.setAttribute("inert", null)
            }
        }
        restoreWebFocus(previouslyFocused)
    }
}

// Claude: stacked modals share siblings; only the last one to close may remove `inert`.
private val inertCounts = HashMap<Element, Int>()

/** Captures the currently focused element so [restoreWebFocus] can return to it later. */
internal expect fun captureWebFocus(): Any?

/** Restores focus to an element previously captured by [captureWebFocus]. */
internal expect fun restoreWebFocus(token: Any?)
