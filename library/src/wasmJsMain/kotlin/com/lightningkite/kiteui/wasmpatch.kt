package com.lightningkite.kiteui

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.js.get

fun JsString.toKString() = toString()

class JsAnyNativeDelegate<T: JsAny, V: Any>(val key: String): ReadWriteProperty<T, V?> {
    override fun getValue(thisRef: T, property: KProperty<*>): V? {
        val ref = (thisRef.get(key) as? JsReference<V>)
        return ref?.get()
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V?) {
        thisRef.set(key, value?.toJsReference())
    }

}
class JsAnyDelegate<T: JsAny, V: JsAny>(val key: String): ReadWriteProperty<T, V?> {
    override fun getValue(thisRef: T, property: KProperty<*>): V? {
//        println("thisRef: $thisRef")
        return thisRef.get(key)?.also { println("it $it") }?.unsafeCast()
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V?) {
        thisRef.set(key, value)
    }

}
public fun <T : JsAny> jsArrayOf(vararg elements: T): JsArray<T> {
    val array = JsArray<T>()
    for (i in elements.indices) {
        array[i] = elements[i]
    }
    return array
}


fun jsObj(): JsAny {
    js("return ({})")
}
private fun jsAny_get(self: JsAny, key: String): JsAny? = js("self[key]")
operator fun JsAny.get(key: String): JsAny? = jsAny_get(this, key)
private fun jsAny_set(self: JsAny, key: String, value: JsAny?): Unit {
    js("self[key] = value")
}
operator fun JsAny.set(key: String, value: JsAny?) = jsAny_set(this, key, value)
fun jsObj(vararg entries: Pair<String, JsAny?>): JsAny {
    val out = jsObj()
    for (entry in entries) out[entry.first] = entry.second
    return out
}
