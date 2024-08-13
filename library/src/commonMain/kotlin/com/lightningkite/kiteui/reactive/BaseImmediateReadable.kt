package com.lightningkite.kiteui.reactive

abstract class BaseImmediateReadable<T>(start: T): ImmediateReadable<T> {
    private val listeners = ArrayList<() -> Unit>()
    override var value: T = start
        set(value) {
            if(field != value) {
                field = value
                onSet(value)
                listeners.invokeAllSafe()
            }
        }

    open fun onSet(value: T) {}

    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
        }
    }
}