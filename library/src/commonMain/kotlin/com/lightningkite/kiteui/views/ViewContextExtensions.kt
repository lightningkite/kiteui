package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope

// by Claude - all rContextAddon defaults write to the root RContext so they're shared across the tree.
// Explicit sets (via the setter) write to the local context, shadowing the root for that subtree.

@Suppress("UNCHECKED_CAST")
fun <T> rContextAddon(init: T): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
    override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
        thisRef.context.addons.getOrPut(property.name) { init } as T

    override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
        thisRef.context.addons[property.name] = value
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> rContextAddonGenerate(init: ViewWriter.() -> T): ReadWriteProperty<ViewWriter, T> =
    object : ReadWriteProperty<ViewWriter, T> {
        override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
            thisRef.context.addons.getOrPut(property.name) { init(thisRef) } as T

        override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
            thisRef.context.addons[property.name] = value
        }
    }

@Suppress("UNCHECKED_CAST")
fun <T> rContextAddonInit(): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
    override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
        thisRef.context.addons.getOrPut(property.name) { throw IllegalStateException("${property.name} has not been initialized. ${thisRef.context}") } as T

    override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
        thisRef.context.addons[property.name] = value
    }
}

@Deprecated(
    "Use 'pageNavigator' instead",
    ReplaceWith("this.pageNavigator", "com.lightningkite.kiteui.navigator.pageNavigator")
)
val ViewWriter.navigator by ViewWriter::pageNavigator

var ViewWriter.safeInsets by rContextAddonGenerate<Reactive<Edges>> { Constant(Edges.ZERO) }

var ViewWriter.popoverParent by rContextAddonGenerate<ViewWriter?> { null }
var ViewWriter.popoverCloser by rContextAddonGenerate<(() -> Unit)?> { null }
var ViewWriter.popoverKeepOpen by rContextAddonGenerate<Int> { 0 }

fun ViewWriter.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closePopovers()
}
fun ViewWriter.closeThisPopover() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closeSiblingPopovers()
}
fun ViewWriter.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}
fun ViewWriter.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

fun ViewWriter.popoverWriter(overlay: ViewWriter = this, popoverRoot: Boolean = false, close: () -> Unit): ViewWriter {
    popoverCloser?.invoke()
    popoverCloser = close
    val writer = object : ViewWriter(), CoroutineScope by this {
        override val representsView: RView? = overlay.representsView
        override val context: RContext = this@popoverWriter.context.split()
        override fun willAddChild(view: RView) = overlay.willAddChild(view)
        override fun addChild(view: RView) = overlay.addChild(view)
    }
    writer.popoverParent = this@popoverWriter.takeIf { !popoverRoot }
    writer.popoverCloser = null
    return writer
}

/**
 * Opens a ViewWriter context that can be used to render overlays. Note that on some platforms, this will spawn a new
 * view tree in the underlying view system. For example, on iOS modal overlays are rendered in a new ViewController,
 * which can be useful when overlaying over a bottom sheet. A side effect of this behavior is that non-modal overlays
 * will appear under bottom sheets on iOS.
 *
 * @param modal `true` if this overlay is intended as a modal, meaning that it covers and _may_ prevent interaction with
 * the UI under the modal.
 *
 * Note that setting this value to true does not enforce modality, but it may opt the layout in
 * to a more appropriate presentation strategy used by the native view system. (This behavior could be enforced using
 * `dismissBackground`, for example.) Setting this value to `false` guarantees that the presentation strategy
 * *will not* prevent interaction with views below the overlay.
 */
expect fun ViewWriter.overlayWriter(
    modal: Boolean = true,
    transition: ScreenTransitions = ScreenTransitions.Fade,
    body: ViewWriter.(remove: () -> Unit) -> Unit
)