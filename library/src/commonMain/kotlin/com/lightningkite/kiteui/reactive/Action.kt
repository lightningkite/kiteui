package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.AppScope
import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.Icon
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

interface Action: Readable<Boolean> {
    val title: String
    val icon: Icon
    fun startAction(scope: CoroutineScope)
}

//data class ExternalLinkAction(
//    override val name: String,
//    override val icon: Icon,
//    val href: String,
//    val newWindow: Boolean
//): Action by (Action("Link", Icon.externalLink) {
//    ExternalServices.openTab(href, newWindow)
//})
//
//data class LinkAction(
//    override val name: String,
//    override val icon: Icon,
//    val to: (() -> Screen)? = null,
//    val newTab: Boolean = false,
//    val resetsStack: Boolean = false,
//): Action by (Action("Link", Icon.externalLink) {
//    TODO()
//})

fun Action(
    title: String,
    icon: Icon,
    clearErrorOnDependencyChange: Boolean = ExceptionHandlers.clearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    log: Console? = null,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend () -> Unit
) = if (clearErrorOnDependencyChange) {
    DependentAction(title, icon, log, keepRunningWhile, ignoreRetryWhileRunning, action = action)
} else {
    RetryableAction(title, icon, log, keepRunningWhile, ignoreRetryWhileRunning, action = action)
}.let {
    frequencyCap?.let { f ->
        FrequencyCapAction(it, f)
    } ?: it
}

class FrequencyCapAction(val wraps: Action, val frequencyCap: Duration = 500.milliseconds) : Action by wraps {
    var lastInvoked = TimeSource.Monotonic.markNow()
    override fun startAction(scope: CoroutineScope) {
        if (lastInvoked.elapsedNow() > frequencyCap) {
            lastInvoked = TimeSource.Monotonic.markNow()
            wraps.startAction(scope)
        }
    }
}

class RetryableAction(
    override val title: String,
    override val icon: Icon,
    val log: Console? = null,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReadable<Boolean> = RawReadable<Boolean>(ReadableState(false)),
    var action: suspend () -> Unit,
) : Action, Readable<Boolean> by reportTo {
    internal var lastJob: Job? = null

    @OptIn(ExperimentalStdlibApi::class)
    override fun startAction(scope: CoroutineScope) {
        if(ignoreRetryWhileRunning && lastJob?.isCompleted == false) return
        lastJob?.cancel()
        lastJob = (keepRunningWhile ?: scope).let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                val result = readableState {
                    action()
                    true
                }
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

    fun cancel() {
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }
}

class DependentAction(
    override val title: String,
    override val icon: Icon,
    override val log: Console? = null,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReadable<Boolean> = RawReadable<Boolean>(ReadableState(false)),
    var action: suspend () -> Unit,
) : DependencyChangeListener(), Action, Readable<Boolean> by reportTo {
    internal var lastJob: Job? = null

    override fun onDependencyNotReady() {
        reportTo.state = ReadableState.NotReady
    }

    override fun onDependencyChange() {
        reportTo.state = ReadableState(false)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun startAction(scope: CoroutineScope) {
        if(ignoreRetryWhileRunning && lastJob?.isCompleted == false) return
        dependencyBlockStart()
        lastJob?.cancel()
        lastJob = (keepRunningWhile ?: scope).let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                val result = readableState {
                    action()
                    true
                }
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
        action = {}
        super.cancel()
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }
}
