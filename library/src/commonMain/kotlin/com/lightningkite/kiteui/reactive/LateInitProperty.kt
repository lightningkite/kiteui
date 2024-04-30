package com.lightningkite.kiteui.reactive

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LateInitProperty<T>() : Writable<T>, ReadWriteProperty<Any?, T>, BaseReadable<T>() {
    var value: T
        get() = state.get()
        set(value) {
            state = ReadableState(value)
        }

    fun unset() {
        state = ReadableState.notReady
    }

    override suspend infix fun set(value: T) {
        this.value = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}