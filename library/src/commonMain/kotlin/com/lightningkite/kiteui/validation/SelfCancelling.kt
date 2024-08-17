package com.virtualrain.selfCancelling

import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.reactive.*

class SelfCancellingContext: CalculationContext, Cancellable {
    private val log = ConsoleRoot.tag("SelfCancelling")
    val debug: Console? = null

    private val onRemoveSet = ArrayList<()->Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveSet.add(action)
    }

    private val onStartupSet = ArrayList<() -> Unit>()
    fun onStartup(action: () -> Unit) {
        onStartupSet.add(action)
    }

    private var trackedListeners = 0

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
    fun track(addListener: () -> (() -> Unit)): () -> Unit {
        val remover = addListener()
        trackedListeners++
        debug?.log("Added listener, current: $trackedListeners")
        startupIfNeeded()

        return {
            remover.invoke()
            trackedListeners--
            debug?.log("Removed listener, current: $trackedListeners")
            cancelIfNotNeeded()
        }
    }

    fun cancelIfNotNeeded() {
        if (trackedListeners > 0) return
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
        override fun addListener(listener: () -> Unit): () -> Unit = context.track { this@trackWith.addListener(listener) }
    }

fun <T> Readable<T>.trackWith(context: SelfCancellingContext): Readable<T> =
    object : Readable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track { this@trackWith.addListener(listener) }
    }

fun <T> Writable<T>.trackWith(context: SelfCancellingContext): Writable<T> =
    object : Writable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track { this@trackWith.addListener(listener) }
    }

fun <T> ImmediateWritable<T>.trackWith(context: SelfCancellingContext): ImmediateWritable<T> =
    object : ImmediateWritable<T> by this {
        override fun addListener(listener: () -> Unit): () -> Unit = context.track { this@trackWith.addListener(listener) }
    }