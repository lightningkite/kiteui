package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.printStackTrace2
import kotlin.random.Random

class SharedReadable<T>(val useLastWhileLoading: Boolean = false, private val action: suspend CalculationContext.() -> T) : Readable<T> {
    val removers = ArrayList<() -> Unit>()
    val ctx = object : CalculationContext {
        override fun onRemove(action: () -> Unit) {
            removers.add(action)
        }
    }
    override var state: ReadableState<T> = ReadableState.notReady
    var listening = false
    var debug: Boolean = false
    val me = Random.nextInt()

    private val listeners = ArrayList<() -> Unit>()
    private var iterating = false

    private fun startupIfNeeded() {
        if (listening) return
        if(debug) println("Starting up $me...")
        listening = true
        ReactiveScopeData(ctx, action = {
            try {
                val result = ReadableState(action(ctx))
                if (result == state) return@ReactiveScopeData
                state = result
            } catch (e: CancelledException) {
                // just bail, since either we're already rerunning or this stuff doesn't matter anymore
                return@ReactiveScopeData
            }catch (e: Exception) {
                state = ReadableState.exception(e)
            }
            iterating = true
            if(debug) println("Informing ${listeners.size} listeners of new state $state from $me...")
            listeners.toList().forEach {
                try {
                    it()
                } catch (e: Exception) {
                    e.printStackTrace2()
                }
            }
            iterating = false
            shutdownIfNotNeeded()
        }, onLoad = {
            if(!useLastWhileLoading) {
                state = ReadableState.notReady
            }
        }, debug = debug)
    }

    private fun shutdownIfNotNeeded() {
        if(listeners.isNotEmpty()) return
        if (!listening) return
        if(debug) println("Shutting down $me...")
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
            if(!iterating) shutdownIfNotNeeded()
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