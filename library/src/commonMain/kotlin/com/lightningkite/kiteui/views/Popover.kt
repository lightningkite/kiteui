@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineScope

/**
 * An open popover, such as a menu or a hint.
 *
 * Only one popover is open at a time, plus any opened from inside it, such as submenus. Opening another popover
 * closes it. Dialogs aren't popovers.
 *
 * Use [ElementContext.currentPopover] to find the open one. Use [ElementContext.enclosingPopover] to find the one
 * an element is in.
 */
public class Popover internal constructor(private val onClose: () -> Unit) {
    internal var closed = false
        private set
    private var openChild: Popover? = null

    /** The popover opened from inside this one, or `null`. */
    public val child: Popover? get() = openChild?.takeUnless { it.closed }

    /** Closes this popover and any opened from inside it. Closing twice does nothing. */
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

private var ElementContext.topPopover: Popover? by contextAddon(null, ContextAddon.Scope.Global)

/**
 * The open popover, or `null` if none is open.
 *
 * A menu item calls `context.currentPopover?.close()` to close the whole menu.
 */
public val ElementContext.currentPopover: Popover? get() = topPopover?.takeUnless { it.closed }

private var ElementContext.inPopover: Popover? by contextAddon(null)

/** The popover this element is in, or `null` if it is in none. */
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
    val writer = object : ViewWriter, ElementWriter by overlay.withChildContext() {}
    writer.context.inPopover = opened
    return writer
}
