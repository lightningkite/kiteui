package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.exceptions.ExceptionHandlersTree
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.reactive.context.DependencyChangeListener
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.context.rerunOn
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
public class ActionInstrumentation(
    public val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    public val onStart: () -> Unit = {},
    public val onEnd: () -> Unit = {},
)

/** Interceptors called on every [Action.startAction]. Each may return an [ActionInstrumentation] to wrap the action. */
public val actionInstrumentors: MutableList<(scope: CoroutineScope, title: String) -> ActionInstrumentation?> = mutableListOf()

public interface Action: Reactive<Boolean> {
    public val title: String
    public val icon: Icon
    public fun startAction(scope: CoroutineScope)
    public operator fun plus(other: Action): Action

    public companion object {
        public var defaultClearErrorOnDependencyChange: Boolean = true
    }
}

public operator fun Action.invoke(scope: CoroutineScope): Unit = startAction(scope)

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
public fun Action(
    title: String,
    icon: Icon = Icon.send,
    clearErrorOnDependencyChange: Boolean = Action.defaultClearErrorOnDependencyChange,
    keepRunningWhile: CoroutineScope? = AppScope,
    frequencyCap: Duration? = 500.milliseconds,
    ignoreRetryWhileRunning: Boolean = true,
    action: suspend CoroutineScope.() -> Unit
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

/**
 * Whether this action exposes the coroutine its [Action.startAction] launched.
 *
 * Only the implementations in this file do. [Action] is a public interface, so a third-party
 * implementation has to be waited on through its reactive state instead - less precise, but the
 * alternative is not waiting at all.
 */
private val Action.exposesItsJob: Boolean
    get() = when (this) {
        is RetryableAction, is DependentAction -> true
        is FrequencyCapAction -> wraps.exposesItsJob
        else -> false
    }

/**
 * The coroutine this action's most recent [Action.startAction] left running, or null.
 *
 * Null when the action finished without ever suspending, when it was never started, when a frequency
 * cap swallowed the start, or when the action does not expose its job at all. It is *not* null when
 * `ignoreRetryWhileRunning` dropped the start: that only happens while a previous run is still going,
 * and waiting for that run is the right thing for a combined action to do.
 */
private val Action.inFlightJob: Job?
    get() = when (this) {
        is RetryableAction -> lastJob?.takeIf { !it.isCompleted }
        is DependentAction -> lastJob?.takeIf { !it.isCompleted }
        is FrequencyCapAction -> wraps.inFlightJob
        else -> null
    }

/**
 * Starts both halves of a combined action, waits for both to finish, then rethrows the first failure.
 *
 * [Action.startAction] is fire-and-forget, so without this the combined action would report
 * completion the instant both starts were issued. The wait is on the coroutines the starts actually
 * left running rather than on each sub-action's reactive state, because that state is not scoped to
 * a single invocation: a [DependentAction] resets it to "ready" as soon as any dependency changes,
 * which would settle the combined action against a run that is still going.
 *
 * Both are started before either is waited on, so they run concurrently, and both are waited on even
 * when the first fails, so the combined action does not settle while the second is still running.
 * The first failure is still the one reported.
 */
private suspend fun startBothAndAwait(scope: CoroutineScope, first: Action, second: Action) {
    val firstBefore = first.state
    first.startAction(scope)
    val firstJob = first.inFlightJob

    val secondBefore = second.state
    second.startAction(scope)
    val secondJob = second.inFlightJob

    val failures = listOfNotNull(
        first.awaitRun(firstBefore, firstJob),
        second.awaitRun(secondBefore, secondJob),
    )
    // A half that genuinely failed outranks a half that was merely cancelled: the real error is the
    // more useful thing to put in front of the user, and reporting the cancellation instead would
    // throw it away.
    (failures.firstOrNull { it !is SubActionCancelledException } ?: failures.firstOrNull())?.let { throw it }
}

/**
 * Reported by a combined action when one half's run was cancelled before finishing.
 *
 * Deliberately not a [CancellationException]: that would cancel the combined action's own coroutine,
 * leaving its state at "not ready" - a spinner that never stops - because `ReactiveState.exception`
 * maps cancellation to not-ready rather than to an error.
 */
public class SubActionCancelledException(message: String) : Exception(message)

/**
 * Waits for the run just started on this action and returns how it failed, or null if it did not.
 *
 * Returns rather than throws so the caller can wait on the other half before deciding what to
 * report; throwing here would abandon a sub-action that is still running.
 *
 * Reading the reactive state alone would be wrong twice over. It is not scoped to one invocation, so
 * a start that a frequency cap swallowed leaves the previous run's result sitting in it - reporting
 * that would fail a combined action for something that never ran. And an action that finished without
 * ever suspending leaves no job to join, so the state is the only evidence there is. Hence both a
 * [joined] job and a state that moved count as "this run produced it".
 */
private suspend fun Action.awaitRun(before: ReactiveState<Boolean>, joined: Job?): Throwable? {
    if (joined == null && !exposesItsJob) {
        // Nothing to join and no way to get one. Fall back to the reactive state, which is what this
        // waited on before jobs were used - imprecise, but far better than returning immediately and
        // reporting a success the action has not earned.
        return try {
            awaitOnce()
            null
        } catch (e: CancellationException) {
            // Our own cancellation, not this sub-action's result. Propagating it immediately is
            // right: there is no longer a combined run for the other half to finish.
            throw e
        } catch (e: Throwable) {
            e
        }
    }
    joined?.join()
    // join() also returns normally for a *cancelled* job, and a sub-action's run is cancelled
    // whenever something else calls startAction on it (startAction cancels lastJob). Without this the
    // combined action would report a clean success for work that was thrown away half-finished.
    if (joined?.isCancelled == true) return SubActionCancelledException("Sub-action '$title' was cancelled")
    return state.takeIf { joined != null || it != before }?.exception
}

public class RetryableAction(
    override val title: String,
    override val icon: Icon,
    public val keepRunningWhile: CoroutineScope? = AppScope,
    public val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive<Boolean>(ReactiveState(false)),
    public val action: suspend CoroutineScope.() -> Unit,
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
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    reportTo.state = ReactiveState.exception(e)
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

    public fun cancel() {
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
    }

    // No reportTo argument: the combined action gets its own state. Sharing the left operand's
    // meant running `a + b` also drove `a`'s own loading/error state, so anything bound to `a`
    // alone showed the combined run's progress and failures.
    override fun plus(other: Action): Action = RetryableAction(
        title,
        icon,
        keepRunningWhile,
        ignoreRetryWhileRunning,
    ) plus@{
        startBothAndAwait(this, this@RetryableAction, other)
    }

    override fun toString(): String = "RetryableAction($title)"
}

public class DependentAction(
    override val title: String,
    override val icon: Icon,
    public val keepRunningWhile: CoroutineScope? = AppScope,
    public val ignoreRetryWhileRunning: Boolean = false,
    private val reportTo: RawReactive<Boolean> = RawReactive(ReactiveState(false)),
    public val action: suspend CoroutineScope.() -> Unit,
) : DependencyChangeListener(), Action, Reactive<Boolean> by reportTo {
    internal var lastJob: Job? = null

    override fun onDependencyNotReady() {
        reportTo.state = ReactiveState.notReady
    }

    override fun onDependencyChange() {
        // Clearing is for the settled result a *finished* run left behind. A dependency moving while
        // the run is still going does not mean it finished, and writing "ready, no error" here would
        // stop the button's spinner and re-enable it mid-flight - visibly wrong for a plain action,
        // and for a combined one (which watches its sub-actions) it would report done the moment the
        // first half completed, with the second still running.
        if (lastJob?.isCompleted == false) return
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
                    done = true
                    reportTo.state = result
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    reportTo.state = ReactiveState.exception(e)
                } finally {
                    dependencyBlockEnd()
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

    // No reportTo argument: the combined action gets its own state. Sharing the left operand's
    // meant running `a + b` also drove `a`'s own loading/error state, so anything bound to `a`
    // alone showed the combined run's progress and failures.
    override fun plus(other: Action): Action = DependentAction(
        title,
        icon,
        keepRunningWhile,
        ignoreRetryWhileRunning,
    ) plus@{
        // A DependentAction clears its error when a tracked dependency changes - that is the whole
        // difference between it and RetryableAction. This lambda reads no reactive state of its own,
        // so without registering the sub-actions it would track nothing and its error would never
        // clear. The sub-actions' own dependencies are tracked by them and surface as state changes,
        // so watching their states relays the whole chain.
        rerunOn(this@DependentAction)
        rerunOn(other)
        startBothAndAwait(this, this@DependentAction, other)
    }

    override fun toString(): String = "DependentAction($title)"
}
