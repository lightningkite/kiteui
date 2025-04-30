package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

var ViewWriter.popoverParent by rContextAddonGenerate<ViewWriter?> { null }
var ViewWriter.popoverCloser by rContextAddonGenerate<(() -> Unit)?> { null }
var ViewWriter.popoverKeepOpen by rContextAddonGenerate<Int> { 0 }

fun ViewWriter.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closePopovers()
}
fun ViewWriter.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}
fun ViewWriter.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

fun ViewWriter.popoverWriter(overlay: ViewWriter = this, close: ()->Unit): ViewWriter {
    popoverCloser?.invoke()
    popoverCloser = close
    val writer = object : ViewWriter(), CalculationContext by this {
        override val context: RContext = this@popoverWriter.context.split()
        override fun willAddChild(view: RView) = overlay.willAddChild(view)
        override fun addChild(view: RView) = overlay.addChild(view)
    }
    writer.popoverParent = this@popoverWriter
    writer.popoverCloser = null
    return writer
}

expect fun ViewWriter.overlayWriter(body: RView.() -> Unit)