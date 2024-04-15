package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.printStackTrace2

class SharedReadable<T>(val useLastWhileLoading: Boolean = false, private val action: suspend CalculationContext.() -> T) : Readable<T> {
    val removers = ArrayList<() -> Unit>()
    val ctx = object : CalculationContext {
        override fun onRemove(action: () -> Unit) {
            removers.add(action)
        }
    }
    override var state: ReadableState<T> = ReadableState.notReady
    var listening = false

    private val listeners = ArrayList<() -> Unit>()

    private fun startupIfNeeded() {
        if (listening) return
        listening = true
        ctx.reactiveScope(onLoad = {
            if(!useLastWhileLoading) state = ReadableState.notReady
        }) {
            try {
                val result = ReadableState(action(ctx))
                if (result == state) return@reactiveScope
                state = result
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
            listeners.toList().forEach {
                try {
                    it()
                } catch (e: Exception) {
                    e.printStackTrace2()
                }
            }
            if(this.listeners.isEmpty()) {
                shutdownIfNotNeeded()
            }
        }
    }

    private fun shutdownIfNotNeeded() {
        if(listeners.isNotEmpty()) return
        if (!listening) return
        listening = false
        removers.forEach {
            try {
                it()
            } catch (e: Exception) {
                e.printStackTrace2()
            }
        }
        state = ReadableState.notReady
    }

    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        startupIfNeeded()
        return {
            removeListener(listener)
        }
    }

    private fun removeListener(listener: () -> Unit) {
        val pos = listeners.indexOfFirst { it === listener }
        if (pos != -1) {
            listeners.removeAt(pos)
            shutdownIfNotNeeded()
        }
    }
}
/**
 * Desired behavior for shared:
 *
 * - Outside a reactive scope, [Readable.await] invokes the action with no sharing
 * - Inside a reactive scope, [Readable.await] starts the whole system listening and sharing the calculation.
 */
fun <T> shared(action: suspend CalculationContext.() -> T): Readable<T> {
    return SharedReadable(action = action)
}
/**
 * Desired behavior for shared:
 *
 * - Outside a reactive scope, [Readable.await] invokes the action with no sharing
 * - Inside a reactive scope, [Readable.await] starts the whole system listening and sharing the calculation.
 */
fun <T> shared(useLastWhileLoading: Boolean, action: suspend CalculationContext.() -> T): Readable<T> {
    return SharedReadable(useLastWhileLoading = useLastWhileLoading, action = action)
}