package com.lightningkite.kiteui.reactive

abstract class BaseReadable<T>(start: ReadableState<T> = ReadableState.notReady): Readable<T> {
    protected val listeners = ArrayList<() -> Unit>()
    override var state: ReadableState<T> = start
        protected set(value) {
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