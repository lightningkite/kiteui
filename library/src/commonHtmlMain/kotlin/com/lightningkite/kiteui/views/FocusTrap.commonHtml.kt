package com.lightningkite.kiteui.views

import com.lightningkite.reactive.context.onRemove

/**
 * Web implementation of [trapFocus].
 *
 * Siblings of this element are marked `inert` so that they cannot be focused or clicked and are
 * hidden from assistive technology. Focus is moved to the first interactive descendant, falling
 * back to this element itself (given a `tabindex` so it can receive focus). Both the `inert`
 * markers and the previously focused element are restored when this element is removed.
 */
actual fun Element.trapFocus() {
    val siblings = parent?.children?.filter { it !== this }.orEmpty()
    val previouslyFocused = captureWebFocus()
    siblings.forEach { it.native.setAttribute("inert", "") }

    // Ensure the scope itself can receive focus if it has no interactive descendants.
    native.setAttribute("tabindex", "-1")
    requestFocusOrDescendant()

    onRemove {
        siblings.forEach { it.native.setAttribute("inert", null) }
        restoreWebFocus(previouslyFocused)
    }
}

/** Captures the currently focused element so [restoreWebFocus] can return to it later. */
internal expect fun captureWebFocus(): Any?

/** Restores focus to an element previously captured by [captureWebFocus]. */
internal expect fun restoreWebFocus(token: Any?)
