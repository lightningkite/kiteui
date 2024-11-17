package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.suspendCoroutineCancellable

internal class WaitForNotNull<T : Any>(val wraps: Readable<T?>) : Readable<T> {

    @Suppress("UNCHECKED_CAST")
    override val state: ReadableState<T>
        get() = when (val s = wraps.state) {
            is ReadableState.Ready -> {
                if (s.value == null) ReadableState.NotReady else s as ReadableState<T>
            }
            is ReadableState.Exception -> s
            is ReadableState.NotReady -> s
        }

    override fun addListener(listener: () -> Unit): () -> Unit {
        return wraps.addListener(listener)
    }

    override fun hashCode(): Int = wraps.hashCode() + 1

    override fun equals(other: Any?): Boolean = other is WaitForNotNull<*> && this.wraps == other.wraps
}
val <T : Any> Readable<T?>.waitForNotNull: Readable<T> get() = WaitForNotNull(this)

suspend fun <T : Any> Readable<T?>.awaitNotNull(): T {
    val basis = await()
    if (basis == null) return suspendCoroutineCancellable<T> { {} }
    else return basis
}