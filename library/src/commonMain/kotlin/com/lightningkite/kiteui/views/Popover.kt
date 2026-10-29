@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineScope

/**
 * An open popover, such as a menu or a hint.
 *
 * One popover is open at a time, along with the popovers opened from inside it, such as submenus; opening another
 * closes it. Dialogs and other overlays aren't popovers. Find the open one with [ElementContext.currentPopover], and
 * the one an element is in with [ElementContext.enclosingPopover].
 */
public class Popover internal constructor(private val onClose: () -> Unit) {
    internal var closed = false
        private set
    private var openChild: Popover? = null

    /** The popover open from inside this one, or `null` if there is none. */
    public val child: Popover? get() = openChild?.takeUnless { it.closed }

    /** Closes this popover and any opened from inside it. Does nothing if it is already closed. */
    public fun close() {
        if (closed) return
        closed = true
        openChild?.close()
        onClose()
    }

    internal fun opened(child: Popover) {
        openChild?.close()
        openChild = child
    }
}

private var ElementContext.topPopover: Popover? by contextAddon(null, global = true)

/**
 * The open popover, or `null` if none is open.
 *
 * Closing it also closes any popovers opened from inside it, so this is the one a menu item closes once picked.
 */
public val ElementContext.currentPopover: Popover? get() = topPopover?.takeUnless { it.closed }

private var ElementContext.inPopover: Popover? by contextAddon(null)

/** The popover this context is inside, or `null` outside any popover. */
public val ElementContext.enclosingPopover: Popover? get() = inPopover?.takeUnless { it.closed }

@Deprecated("Use the popover directly", ReplaceWith("currentPopover?.close()"))
public fun ElementContext.closePopovers() {
    currentPopover?.close()
}

@Deprecated("Use the popover directly", ReplaceWith("enclosingPopover?.close()"))
public fun ElementContext.closeThisPopover() {
    enclosingPopover?.close()
}

@Deprecated("Use the popover directly", ReplaceWith("enclosingPopover?.child?.close()"))
public fun ElementContext.closeSiblingPopovers() {
    val p = enclosingPopover
    if (p != null) p.child?.close() else currentPopover?.close()
}

public fun ElementWriter.popoverWriter(
    overlay: ElementWriter = this,
    popoverRoot: Boolean = false,
    close: () -> Unit
): ViewWriter = context.popoverWriter(overlay, popoverRoot, close)

public fun Element.popoverWriter(overlay: ElementWriter, popoverRoot: Boolean = false, close: () -> Unit): ViewWriter =
    context.popoverWriter(overlay, popoverRoot, close)

public fun ContainerElement.popoverWriter(
    overlay: ElementWriter = this,
    popoverRoot: Boolean = false,
    close: () -> Unit
): ViewWriter = context.popoverWriter(overlay, popoverRoot, close)

private fun ElementContext.popoverWriter(overlay: ElementWriter, popoverRoot: Boolean, close: () -> Unit): ViewWriter {
    val opened = Popover(close)
    val parent = enclosingPopover.takeIf { !popoverRoot }
    if (parent != null) {
        parent.opened(opened)
    } else {
        topPopover?.close()
        topPopover = opened
    }
    val writer = object : ViewWriter, ElementWriter by overlay.split() {}
    writer.context.inPopover = opened
    return writer
}
