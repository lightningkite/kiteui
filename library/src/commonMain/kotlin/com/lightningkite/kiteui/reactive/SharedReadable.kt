@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.report
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

fun <T> shared(coroutineContext: CoroutineContext = Dispatchers.Unconfined, useLastWhileLoading: Boolean = false, action: ReactiveContext.() -> T): Readable<T> {
    return SharedReadable(coroutineContext = coroutineContext, useLastWhileLoading = useLastWhileLoading, action = action)
}

class SharedReadable<T>(
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    useLastWhileLoading: Boolean = false,
    private val action: ReactiveContext.() -> T
) : Readable<T>, CalculationContext {

    private var job = Job()
    private val restOfContext = coroutineContext +
            CoroutineExceptionHandler { coroutineContext, throwable ->
                if (throwable !is CancellationException) {
                    throwable.report("SharedReadable")
                }
            }
    override val coroutineContext = job + restOfContext

    private fun cancel() {
        job.cancel()
        job = Job()
    }

    private val scope = TypedReactiveContext(this, action = action)

    override val state: ReadableState<T>
        get() {
            if (!scope.active) scope.runOnceWhileDead()
            return scope.state
        }
    private var lcount = 0
    override fun addListener(listener: () -> Unit): () -> Unit {
        if (lcount++ == 0) {
            scope.startCalculation()
        }
        val r = scope.addListener(listener)
        var removed = false
        return label@{
            if(removed) return@label
            removed = true
            r()
            if (--lcount == 0) {
                cancel()
            }
        }
    }
}