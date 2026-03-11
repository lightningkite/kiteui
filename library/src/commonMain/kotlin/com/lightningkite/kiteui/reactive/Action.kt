package com.lightningkite.kiteui.reactive

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
import kotlin.coroutines.coroutineContext

interface Action: Reactive<Boolean> {
    val title: String
    val icon: Icon
    fun startAction(scope: CoroutineScope)
    operator fun plus(other: Action): Action
}

operator fun Action.invoke(scope: CoroutineScope) = startAction(scope)

fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = ExceptionHandlers.clearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend CoroutineScope.() -> Unit
) = if (clearErrorOnDependencyChange) {
    DependentAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
} else {
    RetryableAction(title, icon, keepRunningWhile, ignoreRetryWhileRunning, action = action)
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

    override fun plus(other: Action): Action = FrequencyCapAction(wraps.plus(if (other is FrequencyCapAction) other.wraps else other), frequencyCap)
}

class RetryableAction(
    override val title: String,
    override val icon: Icon,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive<Boolean>(ReactiveState(false)),
    val action: suspend CoroutineScope.() -> Unit,
) : Action, Reactive<Boolean> by reportTo {
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

    fun cancel() {
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }

    override fun plus(other: Action) = RetryableAction(
        title,
        icon,
        keepRunningWhile,
        ignoreRetryWhileRunning,
        reportTo
    ) plus@{
        this@RetryableAction.startAction(this)
        other.startAction(this)
    }
}

class DependentAction(
    override val title: String,
    override val icon: Icon,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive<Boolean>(ReactiveState(false)),
    val action: suspend CoroutineScope.() -> Unit,
) : DependencyChangeListener(), Action, Reactive<Boolean> by reportTo {
    internal var lastJob: Job? = null

    override fun onDependencyNotReady() {
        reportTo.state = ReactiveState.notReady
    }

    override fun onDependencyChange() {
        reportTo.state = ReactiveState(false)
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

    override fun cancel() {
        super.cancel()
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }

    override fun plus(other: Action): Action = DependentAction(
        title,
        icon,
        keepRunningWhile,
        ignoreRetryWhileRunning,
        reportTo
    ) plus@{
        this@DependentAction.startAction(this)
        other.startAction(this)
    }
}
