package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.report
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class SharedSuspendingReadable<T>(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    val useLastWhileLoading: Boolean = false,
    var debug: Console? = null,
    private val action: suspend CalculationContext.() -> T
) : BaseReadable<T>(), CalculationContext {
    private var job = SupervisorJob()
    private val restOfContext = (coroutineContext ?: EmptyCoroutineContext) +
            CoroutineExceptionHandler { coroutineContext, throwable ->
                if (throwable !is CancellationException) {
                    throwable.report("SharedSuspendingReadable")
                }
            }
    override val coroutineContext: CoroutineContext get() = restOfContext + job
    private val me = Random.nextInt()

    private var instanceNumber: Int = 1
    override fun activate() {
        super.activate()
        debug?.log("Activating...")
        SuspendingReactiveContext(this, action = {
            try {
                val result = ReadableState(action(this))
                if (result == state) return@SuspendingReactiveContext
                state = result
            } catch (e: CancelledException) {
                // just bail, since either we're already rerunning or this stuff doesn't matter anymore
                return@SuspendingReactiveContext
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
        }, onLoad = {
            if(!useLastWhileLoading) {
                state = ReadableState.notReady
            }
        }, debug = debug?.tag((instanceNumber++).toString()))
    }

    override fun deactivate() {
        super.deactivate()
        debug?.log("Deactivating...")
        job.cancel()
        job = SupervisorJob()
        state = ReadableState.notReady
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