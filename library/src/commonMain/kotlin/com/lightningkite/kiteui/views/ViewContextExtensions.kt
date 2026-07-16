package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.identityHashCode
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope

// by Claude - all rContextAddon defaults write to the root RContext so they're shared across the tree.
// Explicit sets (via the setter) write to the local context, shadowing the root for that subtree.

class ContextAddon<T>(val init: Init<T>) {
    fun interface Init<out T> {
        fun get(context: ElementContext, property: KProperty<*>): T

        data class Value<T>(val value: T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = value
        }
        data class Lazy<T>(val init: (ElementContext) -> T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = init(context)
        }
        data object LateInit : Init<Nothing> {
            override fun get(context: ElementContext, property: KProperty<*>): Nothing =
                throw IllegalStateException("late-init addon '${property.name}' has not been initialized.")
        }
    }

    constructor(value: T) : this(Init.Value(value))
    constructor(init: (ElementContext) -> T) : this(Init.Lazy(init))

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: ElementContext, property: KProperty<*>): T =
        thisRef.addons.getOrPut(property.name) { init.get(thisRef, property) } as T

    operator fun setValue(thisRef: ElementContext, property: KProperty<*>, value: T) {
        thisRef.addons[property.name] = value
    }
}

fun <T> contextAddon(init: T) = ContextAddon(init)
fun <T> lateInitContextAddon() = ContextAddon<T>(ContextAddon.Init.LateInit)
fun <T> lazyContextAddon(init: (ElementContext) -> T) = ContextAddon(init)

@Deprecated("Renamed to reflect change in receiver", ReplaceWith("contextAddon(init)"))
fun <T> rContextAddon(init: T) = contextAddon(init)

@Deprecated("Renamed to reflect change in receiver", ReplaceWith("lazyContextAddon(init)"))
fun <T> rContextAddonGenerate(init: (ElementContext) -> T) = lazyContextAddon(init)

@Deprecated("Renamed to reflect change in receiver", ReplaceWith("lateInitContextAddon()"))
fun <T> rContextAddonInit() = lateInitContextAddon<T>()

var ElementContext.safeInsets by lazyContextAddon<Reactive<Edges>> { Constant(Edges.ZERO) }

var ElementContext.popoverParent by lazyContextAddon<ContainerElement?> { null }
var ElementContext.popoverCloser by lazyContextAddon<(() -> Unit)?> { null }
var ElementContext.popoverKeepOpen by lazyContextAddon { 0 }

// Stack of dismiss lambdas for currently-open dismissable modal dialogs (topmost is last). Shared
// app-wide because a lazyContextAddon's default is created once and stored on the ROOT context, so
// every context in the tree resolves to the same list. This lets back handlers (Android system back,
// browser back) dismiss the top dialog before navigating pages.
private var ElementContext.dismissableDialogStack by lazyContextAddon { mutableListOf<() -> Unit>() }

/** Registers [dismiss] as the topmost open dialog; returns a lambda that unregisters it on close. */
fun ElementContext.pushDismissableDialog(dismiss: () -> Unit): Release {
    val stack = dismissableDialogStack
    stack.add(dismiss)
    return { stack.remove(dismiss) }
}

/** If a dismissable dialog is open, dismisses the topmost one and returns true; otherwise false. */
fun ElementContext.dismissTopDialog(): Boolean {
    val top = dismissableDialogStack.removeLastOrNull() ?: return false
    top()
    return true
}

fun ElementContext.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.context?.closePopovers()
}

fun ElementContext.closeThisPopover() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.context?.closeSiblingPopovers()
}

fun ElementContext.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}

fun ElementContext.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

fun ElementWriter.popoverWriter(
    overlay: ElementWriter = this,
    popoverRoot: Boolean = false,
    close: () -> Unit
): ViewWriter {
    context.popoverCloser?.invoke()
    context.popoverCloser = close

    val writer = object : ViewWriter, ElementWriter by overlay.split() {}

    writer.context.popoverParent = (this@popoverWriter as? ContainerElement)?.takeIf { !popoverRoot }
    writer.context.popoverCloser = null
    writer.context.popoverKeepOpen = 0

    return writer
}

fun Element.popoverWriter(overlay: ElementWriter, popoverRoot: Boolean = false, close: () -> Unit): ViewWriter {
    context.popoverCloser?.invoke()
    context.popoverCloser = close

    val writer = object : ViewWriter, ElementWriter by overlay.split() {}

    writer.context.popoverParent = (this@popoverWriter as? ContainerElement)?.takeIf { !popoverRoot }
    writer.context.popoverCloser = null
    writer.context.popoverKeepOpen = 0

    return writer
}

fun ContainerElement.popoverWriter(
    overlay: ElementWriter = this,
    popoverRoot: Boolean = false,
    close: () -> Unit
): ViewWriter {
    context.popoverCloser?.invoke()
    context.popoverCloser = close

    val writer = object : ViewWriter, ElementWriter by overlay.split() {}

    writer.context.popoverParent = this@popoverWriter.takeIf { !popoverRoot }
    writer.context.popoverCloser = null
    writer.context.popoverKeepOpen = 0

    return writer
}

/**
 * Opens a ContainerElement context that can be used to render overlays. Note that on some platforms, this will spawn a new
 * view tree in the underlying view system. For example, on iOS modal overlays are rendered in a new ViewController,
 * which can be useful when overlaying over a bottom sheet. A side effect of this behavior is that non-modal overlays
 * will appear under bottom sheets on iOS.
 *
 * @param modal `true` if this overlay is intended as a modal, meaning that it covers and _may_ prevent interaction with
 * the UI under the modal. Note that setting this value to true does not enforce modality, but it may opt the layout in
 * to a more appropriate presentation strategy used by the native view system. (This behavior could be enforced using
 * `dismissBackground`, for example.) Setting this value to `false` guarantees that the presentation strategy
 * *will not* prevent interaction with views below the overlay.
 *
 * @param navClosable if `true` this overlay will be closable with the platform's 'back' action.
 */
// TODO: Need to implement `navClosable` on ios
expect fun ElementContext.overlay(
    modal: Boolean = true,
    navClosable: Boolean = modal,
    transition: ScreenTransitions = ScreenTransitions.Fade,
    body: ContainerElement.(remove: () -> Unit) -> Unit
)