package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope

@Suppress("UNCHECKED_CAST")
public fun <T> rContextAddon(init: T): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
    override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
        thisRef.context.addons.getOrPut(property.name) { init } as T

    override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
        thisRef.context.addons[property.name] = value
    }
}

@Suppress("UNCHECKED_CAST")
public fun <T> rContextAddonGenerate(init: ViewWriter.() -> T): ReadWriteProperty<ViewWriter, T> =
    object : ReadWriteProperty<ViewWriter, T> {
        override fun getValue(thisRef: ViewWriter, property: KProperty<*>): T =
            thisRef.context.addons.getOrPut(property.name) { init(thisRef) } as T

        override fun setValue(thisRef: ViewWriter, property: KProperty<*>, value: T) {
            thisRef.context.addons[property.name] = value
        }
    }

@Suppress("UNCHECKED_CAST")
public fun <T> rContextAddonInit(): ReadWriteProperty<ViewWriter, T> = object : ReadWriteProperty<ViewWriter, T> {
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
public val ViewWriter.navigator: PageNavigator by ViewWriter::pageNavigator

public var ViewWriter.safeInsets: Reactive<Edges> by rContextAddonGenerate<Reactive<Edges>> { Constant(Edges.ZERO) }

public var ViewWriter.popoverParent: ViewWriter? by rContextAddonGenerate<ViewWriter?> { null }
public var ViewWriter.popoverCloser: (() -> Unit)? by rContextAddonGenerate<(() -> Unit)?> { null }
public var ViewWriter.popoverKeepOpen: Int by rContextAddonGenerate<Int> { 0 }

public fun ViewWriter.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closePopovers()
}
public fun ViewWriter.closeThisPopover() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closeSiblingPopovers()
}
public fun ViewWriter.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}
public fun ViewWriter.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

public fun ViewWriter.popoverWriter(overlay: ViewWriter = this, popoverRoot: Boolean = false, close: ()->Unit): ViewWriter {
    popoverCloser?.invoke()
    popoverCloser = close
    val writer = object : ViewWriter(), CalculationContext by this {
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
public expect fun ViewWriter.overlayWriter(
    modal: Boolean = true,
    transition: ScreenTransitions = ScreenTransitions.Fade,
    body: RView.(remove: () -> Unit) -> Unit
)