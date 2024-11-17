package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import kotlinx.coroutines.*

class SuspendingReactiveContext<T> constructor(
    val scope: CoroutineScope,
    var action: suspend () -> T,
    override val log: Console? = null,
    private val reportTo: RawReadable<T> = RawReadable<T>(),
) : DependencyChangeListener(), Readable<T> by reportTo {
    internal var lastJob: Job? = null

    override fun onDependencyNotReady() {
        reportTo.state = ReadableState.NotReady
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onDependencyChange() {
        log?.log("onDependencyChange")
        lastJob?.cancel()
        dependencyBlockStart()

        lastJob = (scope + this).let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                log?.log("calculation started")
                val result = readableState {
                    action()
                }
                log?.log("result: $result")
                dependencyBlockEnd()
                done = true
                reportTo.state = result
            }

            if (done) {
                return@let null
            } else {
                // start load
                reportTo.state = ReadableState.NotReady
                return@let job
            }
        }
    }

    override fun cancel() {
        log?.log("shutdown")
        super.cancel()
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }

    init {
        onDependencyChange()
        scope.onRemove {
            cancel()
        }
    }
}

@Suppress("NOTHING_TO_INLINE") inline fun CalculationContext.reactiveSuspending(log: Console? = null, noinline action: suspend () -> Unit) = SuspendingReactiveContext(this, action, log).also {
    coroutineContext[StatusListener.Key]?.loading(it)
}

inline fun CalculationContext.reactiveSuspending(log: Console? = null, crossinline onLoad: () -> Unit, noinline action: suspend () -> Unit): SuspendingReactiveContext<Unit> {
    return reactiveSuspending(log, action = action).also {
        it.addListener { if(!it.state.ready) onLoad() }
    }
}