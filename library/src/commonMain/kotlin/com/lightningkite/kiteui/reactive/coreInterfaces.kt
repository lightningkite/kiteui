package com.lightningkite.kiteui.reactive

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface ResourceUse {
    fun start(): () -> Unit
}

interface Listenable : ResourceUse {
    /**
     * Adds the [listener] to be called every time this event fires.
     * @return a function to remove the [listener] that was added.  Removing multiple times should not cause issues.
     */
    fun addListener(listener: () -> Unit): () -> Unit
    override fun start(): () -> Unit = addListener { }
}

interface Readable<out T> : Listenable {
    val state: ReadableState<T>
}

interface WriteOnly<T> {
    suspend infix fun set(value: T)
}

interface Writable<T> : Readable<T>, WriteOnly<T>

interface ImmediateReadable<out T> : Readable<T>, ReadOnlyProperty<Any?, T> {
    val value: T
    override val state: ReadableState<T> get() = ReadableState(value)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

interface ImmediateWriteOnly<T>: WriteOnly<T> {
    fun setImmediate(value: T)
    override suspend fun set(value: T) { setImmediate(value) }
}

interface ImmediateReadableWithWrite<T> : Writable<T>, ImmediateReadable<T>
interface ReadableWithImmediateWrite<T> : Writable<T>, ImmediateWriteOnly<T>

interface ImmediateWritable<T> : ImmediateWriteOnly<T>, ImmediateReadable<T>, Writable<T>, ReadWriteProperty<Any?, T> {
    override var value: T
    override fun setImmediate(value: T) { this.value = value }
    override suspend fun set(value: T) { this.value = value }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class NotReadyException(message: String? = null) : IllegalStateException(message)

