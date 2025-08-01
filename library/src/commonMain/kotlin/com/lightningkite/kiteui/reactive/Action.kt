package com.lightningkite.kiteui.reactive

import com.lightningkite.signal.*
import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.*

public interface Action: Reactive<Boolean> {
    public val title: String
    public val icon: Icon
    public fun startAction(scope: CoroutineScope)
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
//    val to: (() -> Page)? = null,
//    val newTab: Boolean = false,
//    val resetsStack: Boolean = false,
//): Action by (Action("Link", Icon.externalLink) {
//    TODO()
//})

public fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = ExceptionHandlers.clearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend () -> Unit
): Action = if (clearErrorOnDependencyChange) {
    DependentAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
} else {
    RetryableAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
}.let {
    frequencyCap?.let { f ->
        FrequencyCapAction(it, f)
    } ?: it
}

public class FrequencyCapAction(public val wraps: Action, public val frequencyCap: Duration = 500.milliseconds) : Action by wraps {
    public var lastInvoked: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
    public override fun startAction(scope: CoroutineScope) {
        if (lastInvoked.elapsedNow() > frequencyCap) {
            lastInvoked = TimeSource.Monotonic.markNow()
            wraps.startAction(scope)
        }
    }
}

public class RetryableAction(
    public override val title: String,
    public override val icon: Icon,
    public val keepRunningWhile: CoroutineScope? = AppScope,
    public val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReadable<Boolean> = RawReadable<Boolean>(ReadableState(false)),
    public var action: suspend () -> Unit,
) : Action, Reactive<Boolean> by reportTo {
    internal var lastJob: Job? = null

    @OptIn(ExperimentalStdlibApi::class)
    public override fun startAction(scope: CoroutineScope) {
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
                val result = reactiveState {
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
                reportTo.state = ReactiveState.notReady
                return@let job
            }
        }
    }

    public fun cancel() {
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }
}

class DependentAction(
    override val title: String,
    override val icon: Icon,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive<Boolean>(ReactiveState(false)),
    var action: suspend () -> Unit,
) : DependencyChangeListener(), Action, Reactive<Boolean> by reportTo {
public class DependentAction(
    public override val title: String,
    public override val icon: Icon,
    public val keepRunningWhile: CoroutineScope? = AppScope,
    public val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReadable<Boolean> = RawReadable<Boolean>(ReadableState(false)),
    public var action: suspend () -> Unit,
) : DependencyChangeListener(), Action, Readable<Boolean> by reportTo {
    internal var lastJob: Job? = null

    override fun onDependencyNotReady() {
        reportTo.state = ReactiveState.notReady
    public override fun onDependencyNotReady() {
        reportTo.state = ReadableState.notReady
    }

    override fun onDependencyChange() {
        reportTo.state = ReactiveState(false)
    public override fun onDependencyChange() {
        reportTo.state = ReadableState(false)
    }

    @OptIn(ExperimentalStdlibApi::class)
    public override fun startAction(scope: CoroutineScope) {
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
                val result = reactiveState {
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
                reportTo.state = ReactiveState.notReady
                return@let job
            }
        }
    }

    public override fun cancel() {
        action = {}
        super.cancel()
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }
}
