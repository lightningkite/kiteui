package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.exceptions.ExceptionHandlersTree
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.reactive.context.DependencyChangeListener
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Returned by [actionInstrumentors] to wrap an action's execution with telemetry or other instrumentation.
 * @param coroutineContext added to the launched coroutine (e.g. trace/span propagation)
 * @param onStart called at the start of the action coroutine (e.g. to set active span globals)
 * @param onEnd called in `finally` when the action completes or is cancelled
 */
class ActionInstrumentation(
    val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    val onStart: () -> Unit = {},
    val onEnd: () -> Unit = {},
)

/** Interceptors called on every [Action.startAction]. Each may return an [ActionInstrumentation] to wrap the action. */
val actionInstrumentors: MutableList<(scope: CoroutineScope, title: String) -> ActionInstrumentation?> = mutableListOf()

interface Action: Reactive<Boolean> {
    val title: String
    val icon: Icon
    fun startAction(scope: CoroutineScope)
    operator fun plus(other: Action): Action

    companion object {
        var defaultClearErrorOnDependencyChange: Boolean = true
    }
}

operator fun Action.invoke(scope: CoroutineScope) = startAction(scope)

/**
 * Creates an [Action] that wraps [action] with optional frequency capping.
 *
 * The returned type depends on [clearErrorOnDependencyChange]:
 * - `true` (default): returns a [DependentAction] — the error/loading state is cleared whenever
 *   a reactive dependency changes. Use this when the action's validity depends on reactive state
 *   (e.g., a "Save" button whose enabled state tracks a form).
 * - `false`: returns a [RetryableAction] — the error state persists until the user retries or
 *   the action succeeds. Use this for idempotent operations where the user should see a persistent
 *   error and decide whether to retry.
 *
 * Both variants are wrapped in a [FrequencyCapAction] when [frequencyCap] is non-null (default 500 ms),
 * preventing accidental double-submissions from rapid taps.
 *
 * @param clearErrorOnDependencyChange whether reactive dependency changes clear the action's error state
 * @param keepRunningWhile coroutine scope that keeps the action alive; defaults to [AppScope]
 * @param frequencyCap minimum time between allowed invocations; null disables the cap
 * @param ignoreRetryWhileRunning if true, additional [startAction] calls are dropped while the action is in progress
 */
fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = Action.defaultClearErrorOnDependencyChange,
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
    // initialize in the past so the first invocation is never suppressed
    private var lastInvoked = TimeSource.Monotonic.markNow() - frequencyCap - 1.milliseconds

    override fun startAction(scope: CoroutineScope) {
        if (lastInvoked.elapsedNow() > frequencyCap) {
            lastInvoked = TimeSource.Monotonic.markNow()
            wraps.startAction(scope)
        }
    }

    override fun plus(other: Action): Action = FrequencyCapAction(wraps.plus(if (other is FrequencyCapAction) other.wraps else other), frequencyCap)

    override fun toString(): String = "FrequencyCapAction($wraps)"
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
        val instrumentations = actionInstrumentors.mapNotNull { it(scope, title) }
        val extraContext = instrumentations.fold<ActionInstrumentation, CoroutineContext>(EmptyCoroutineContext) { acc, it -> acc + it.coroutineContext }
        lastJob = (keepRunningWhile ?: scope).let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                context = extraContext,
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                instrumentations.forEach { it.onStart() }
                try {
                    val result = reactiveState {
                        action()
                        true
                    }
                    done = true
                    reportTo.state = result
                } finally {
                    instrumentations.forEach { it.onEnd() }
                }
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

    override fun toString(): String = "RetryableAction($title)"
}

class DependentAction(
    override val title: String,
    override val icon: Icon,
    val keepRunningWhile: CoroutineScope? = AppScope,
    val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive(ReactiveState(false)),
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
        if (ignoreRetryWhileRunning && lastJob?.isCompleted == false) return
        dependencyBlockStart()
        lastJob?.cancel()
        val instrumentations = actionInstrumentors.mapNotNull { it(scope, title) }
        val extraContext = instrumentations.fold<ActionInstrumentation, CoroutineContext>(EmptyCoroutineContext) { acc, it -> acc + it.coroutineContext }
        lastJob = (keepRunningWhile ?: scope).let { calculationContext ->
            var done = false
            val job = calculationContext.launch(
                context = extraContext + this,
                start = if (calculationContext.coroutineContext[CoroutineDispatcher]?.isDispatchNeeded(
                        calculationContext.coroutineContext
                    ) == false
                ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
            ) {
                instrumentations.forEach { it.onStart() }
                try {
                    val result = reactiveState {
                        action()
                        true
                    }
                    dependencyBlockEnd()
                    done = true
                    reportTo.state = result
                } finally {
                    instrumentations.forEach { it.onEnd() }
                }
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

    override fun toString(): String = "DependentAction($title)"
}
