package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.reactive.*

class SelfCancellingContext: CalculationContext, Cancellable {
    var debug: Console? = null

    private val onRemoveSet = ArrayList<()->Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveSet.add(action)
    }

    private val onStartupSet = ArrayList<() -> Unit>()
    fun onStartup(action: () -> Unit) {
        onStartupSet.add(action)
    }

    private val trackedListeners = ArrayList<() -> Unit>()

    var active = false
        private set

    fun startupIfNeeded() {
        if (active) return
        debug?.log("Starting up")
        active = true
        onStartupSet.invokeAllSafe()
    }

    /**
     * Override addListener() on the listenable with this function
    * */
    fun track(listenable: Listenable, listener: () -> Unit): () -> Unit {
        val remover = listenable.addListener(listener)
        trackedListeners.add(listener)
        debug?.log("Added listener, current: ${trackedListeners.size}")
        startupIfNeeded()

        return {
            remover.invoke()
            removeTracked(listener)
        }
    }
    fun track(listener: () -> Unit, addListener: (() -> Unit) -> () -> Unit): () -> Unit {
        val remover = addListener(listener)
        trackedListeners.add(listener)
        debug?.log("Added listener, current: ${trackedListeners.size}")
        startupIfNeeded()

        return {
            remover.invoke()
            removeTracked(listener)
        }
    }

    private fun removeTracked(listener: () -> Unit) {
        val pos = trackedListeners.indexOfFirst { it === listener }
        if (pos != -1) {
            trackedListeners.removeAt(pos)
            debug?.log("Removed Listener, remaining: ${trackedListeners.size}")
            cancelIfNotNeeded()
        }
    }

    fun cancelIfNotNeeded() {
        if (trackedListeners.isNotEmpty()) return
        if (!active) return
        active = false
        cancel()
    }

    override fun cancel() {
        debug?.log("Cancelling")
        onRemoveSet.invokeAllSafe()
    }
}

fun Listenable.trackWith(context: SelfCancellingContext): Listenable =
    object : Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track(this@trackWith, listener)
    }

fun <T> Readable<T>.trackWith(context: SelfCancellingContext): Readable<T> =
    object : Readable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track(this@trackWith, listener)
    }

fun <T> ImmediateReadable<T>.trackWith(context: SelfCancellingContext): ImmediateReadable<T> =
    object : ImmediateReadable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track(this@trackWith, listener)
    }

fun <T> Writable<T>.trackWith(context: SelfCancellingContext): Writable<T> =
    object : Writable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track(this@trackWith, listener)
    }

fun <T> ImmediateWritable<T>.trackWith(context: SelfCancellingContext): ImmediateWritable<T> =
    object : ImmediateWritable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track(this@trackWith, listener)
    }