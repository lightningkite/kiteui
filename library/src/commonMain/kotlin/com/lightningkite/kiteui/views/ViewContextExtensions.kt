package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.l2.overlayStack
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
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
    "Use 'screenNavigator' instead",
    ReplaceWith("this.screenNavigator", "com.lightningkite.kiteui.navigator.screenNavigator")
)
val ViewWriter.navigator by ViewWriter::screenNavigator

var ViewWriter.rootPopoverCloser by rContextAddon(BasicListenable())
var ViewWriter.popoverClosers by rContextAddonGenerate { rootPopoverCloser }
fun ViewWriter.closePopovers() {
    rootPopoverCloser.invokeAll()
}

fun ViewWriter.closeSiblingPopovers() {
    popoverClosers.invokeAll()
}

fun ViewWriter.popoverWriter(close: ()->Unit): ViewWriter {
    val writer = object : ViewWriter() {
        override val context: RContext = this@popoverWriter.context.split()
        override fun addChild(view: RView) = this@popoverWriter.addChild(view)
    }
    this@popoverWriter.closeSiblingPopovers()
    val childCloser = BasicListenable()
    var closeCurrent = {}
    var stopListeningToCloser = {}
    fun internalClose() {
        stopListeningToCloser()
        closeCurrent()
        close()
    }
    stopListeningToCloser = this@popoverWriter.popoverClosers.addListener {
        childCloser.invokeAll()
        internalClose()
    }
    writer.popoverClosers = childCloser
    return writer
}