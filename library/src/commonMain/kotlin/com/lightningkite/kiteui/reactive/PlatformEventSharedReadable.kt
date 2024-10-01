package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.report

class PlatformEventSharedReadable<T>(
    private val startup: (update: (T) -> Unit) -> T,
    private val shutdown: () -> Unit
) : Readable<T> {
    val removers = ArrayList<() -> Unit>()
    override var state: ReadableState<T> = ReadableState.notReady
    var listening = false

    private val listeners = ArrayList<() -> Unit>()
    private var iterating = false

    private fun notifyListeners() {
        iterating = true
        listeners.invokeAllSafe()
        iterating = false
    }

    private fun startupIfNeeded() {
        if (listening) return
        listening = true
        val startingValue = startup {
            state = ReadableState(it)
            notifyListeners()
            shutdownIfNotNeeded()
        }
        state = ReadableState(startingValue)
    }

    private fun shutdownIfNotNeeded() {
        if(listeners.isNotEmpty()) return
        if (!listening) return
        listening = false
        removers.forEach {
            try {
                it()
            } catch (e: Exception) {
                e.report()
            }
        }
        state = ReadableState.notReady
        shutdown()
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
            if(!iterating) shutdownIfNotNeeded()
        }
    }
}