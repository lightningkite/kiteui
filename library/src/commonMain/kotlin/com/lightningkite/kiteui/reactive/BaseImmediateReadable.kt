package com.lightningkite.kiteui.reactive

abstract class BaseImmediateReadable<T>(start: T): ImmediateReadable<T> {
    private val listeners = ArrayList<() -> Unit>()
    override var value: T = start
        set(value) {
            if(field != value) {
                field = value
                listeners.invokeAllSafe()
            }
        }

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