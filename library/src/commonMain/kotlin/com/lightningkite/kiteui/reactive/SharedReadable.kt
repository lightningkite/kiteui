@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.printStackTrace2
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

fun <T> shared(coroutineContext: CoroutineContext = Dispatchers.Default, useLastWhileLoading: Boolean = false, action: ReactiveContext.() -> T): Readable<T> {
    return SharedReadable(coroutineContext = coroutineContext, useLastWhileLoading = useLastWhileLoading, action = action)
}

class SharedReadable<T>(coroutineContext: CoroutineContext = Dispatchers.Default, useLastWhileLoading: Boolean = false, private val action: ReactiveContext.() -> T) :
    Readable<T>, CalculationContext {

    private var job = Job()
    override val coroutineContext = (coroutineContext ?: EmptyCoroutineContext) +
            job +
            CoroutineExceptionHandler { coroutineContext, throwable ->
            if (throwable !is CancellationException) {
                throwable.printStackTrace2()
            }
        }

    private fun cancel() {
        job.cancel()
        job = Job()
    }

    override fun notifyStart() {
        super.notifyStart()
        if (lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }

    override fun notifyComplete(result: Result<Unit>) {
        super.notifyComplete(result)
        if (lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }

    override fun notifyLongComplete(result: Result<Unit>) {
        super.notifyLongComplete(result)
        if (lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }

    private val scope = DirectReactiveContext(this, scheduled = false, action = action)
    override val state: ReadableState<T>
        get() {
            if (!scope.active) scope.runOnceWhileDead()
            return scope.lastResult
        }
    private var lastNotified: ReadableState<T> = ReadableState.notReady
    val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        if (listeners.size == 0) {
            scope.start()
        }
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
            if (listeners.size == 0) {
                cancel()
            }
        }
    }
}