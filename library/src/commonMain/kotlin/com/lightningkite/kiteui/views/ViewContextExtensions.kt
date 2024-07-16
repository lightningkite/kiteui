package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget
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
