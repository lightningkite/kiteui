package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.report
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class SharedSuspendingReadable<T>(coroutineContext: CoroutineContext = Dispatchers.Default, val useLastWhileLoading: Boolean = false, private val action: suspend CalculationContext.() -> T) : Readable<T> {
    var job = Job()
    val ctx = object: CalculationContext {
        override val coroutineContext = (coroutineContext ?: EmptyCoroutineContext) +
                job +
                CoroutineExceptionHandler { coroutineContext, throwable ->
                    if (throwable !is CancellationException) {
                        throwable.printStackTrace2()
                    }
                }
    }
    override var state: ReadableState<T> = ReadableState.notReady
    var listening = false
    var debug: Console? = null
    val me = Random.nextInt()

    private val listeners = ArrayList<() -> Unit>()
    private var iterating = false

    private fun notifyListeners() {
        iterating = true
        debug?.log("Informing ${listeners.size} listeners of new state $state...")
        listeners.invokeAllSafe()
        iterating = false
    }

    private fun startupIfNeeded() {
        if (listening) return
        debug?.log("Starting up...")
        listening = true
        SuspendingReactiveContext(ctx, action = {
            try {
                val result = ReadableState(action(ctx))
                if (result == state) return@SuspendingReactiveContext
                state = result
            } catch (e: CancelledException) {
                // just bail, since either we're already rerunning or this stuff doesn't matter anymore
                return@SuspendingReactiveContext
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
            notifyListeners()
            shutdownIfNotNeeded()
        }, onLoad = {
            if(!useLastWhileLoading) {
                state = ReadableState.notReady
                notifyListeners()
            }
        }, debug = debug)
    }

    private fun shutdownIfNotNeeded() {
        if(listeners.isNotEmpty()) return
        if (!listening) return
        debug?.log("Shutting down...")
        listening = false
        job.cancel()
        job = Job()
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
fun <T> sharedSuspending(coroutineContext: CoroutineContext = Dispatchers.Default, useLastWhileLoading: Boolean = false, action: suspend CalculationContext.() -> T): Readable<T> {
    return SharedSuspendingReadable(coroutineContext = coroutineContext, useLastWhileLoading = useLastWhileLoading, action = action)
}