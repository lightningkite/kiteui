package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.testing.BaseUiTest
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.RawReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for [RetryableAction.plus] and [DependentAction.plus]: the combined action must
 * not report completion until BOTH sub-actions have actually finished, must still run the second
 * sub-action to completion even when the first one fails, and must propagate the first sub-action's
 * failure (not the second's success) as its own result.
 *
 * Before the fix, the combined action's lambda fired both `startAction()` calls and returned without
 * awaiting either sub-action's terminal state - since `startAction()` is fire-and-forget, the combined
 * action settled the instant both starts were issued, and a first-sub-action failure could abandon the
 * second before it ran.
 *
 * Each sub-action is parked on a [CompletableDeferred] gate so its completion is under the test's control.
 *
 * The combined action also has its own state holder rather than sharing the left operand's, and it waits
 * on the coroutines the two starts actually left running rather than on the sub-actions' reactive state.
 * Both are load-bearing and covered below: sharing meant `a + b` drove `a`'s own state, and a sub-action's
 * state is not scoped to one invocation - it moves when the sub-action merely starts or finishes, so
 * treating any movement as "the combined run is over" settles it the moment the first half lands.
 *
 * [DependentAction.onDependencyChange] is tested here too, in its own right rather than only through
 * `plus`: it now leaves a run that is still in flight alone, since clearing an error is about the result
 * a *finished* run left behind.
 */
class ActionCombineTest : BaseUiTest() {

    @Test
    fun retryableActionPlusAwaitsBothSubActionsAndPropagatesFirstFailure() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        var secondRan = false

        val first = RetryableAction("first", Icon.send, keepRunningWhile = scope) {
            firstGate.await()
            throw IllegalStateException("first failed")
        }
        val second = RetryableAction("second", Icon.send, keepRunningWhile = scope) {
            secondGate.await()
            secondRan = true
        }

        val combined = first.plus(second)
        combined.startAction(scope)

        // Both sub-actions are parked on their gates - the combined action must not have settled yet.
        assertFalse(secondRan, "second sub-action ran before its gate was released")

        firstGate.complete(Unit)
        // The first sub-action has now failed. The second must still be allowed to run - abandoning it
        // on the first failure is exactly the regression this test exists to catch.
        assertFalse(secondRan, "second sub-action ran before its gate was released")

        secondGate.complete(Unit)

        assertTrue(secondRan, "second sub-action never ran to completion")
        assertEquals(
            "first failed",
            combined.state.exception?.message,
            "combined action should propagate the first sub-action's failure"
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun dependentActionPlusAwaitsBothSubActionsAndPropagatesFirstFailure() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        var secondRan = false

        // Typed as Action (not DependentAction) so `first.plus(second)` resolves to Action.plus -
        // DependentAction also implements CoroutineContext.Element (see its use as `+ this` in
        // startAction()), which otherwise makes `plus` ambiguous with CoroutineContext.plus.
        val first: Action = DependentAction("first", Icon.send, keepRunningWhile = scope) {
            firstGate.await()
            throw IllegalStateException("first failed")
        }
        val second: Action = DependentAction("second", Icon.send, keepRunningWhile = scope) {
            secondGate.await()
            secondRan = true
        }

        val combined = first.plus(second)
        combined.startAction(scope)

        assertFalse(secondRan, "second sub-action ran before its gate was released")

        firstGate.complete(Unit)
        // As above: a failing first sub-action must not cancel the second.
        assertFalse(secondRan, "second sub-action ran before its gate was released")

        secondGate.complete(Unit)

        assertTrue(secondRan, "second sub-action never ran to completion")
        assertEquals(
            "first failed",
            combined.state.exception?.message,
            "combined action should propagate the first sub-action's failure"
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun combinedActionDoesNotDriveTheLeftOperandsOwnState() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)

        // The failure is on the RIGHT this time, so the two states must end up saying different
        // things: `first` succeeded, `combined` failed. While `plus` passed the receiver's own
        // holder through, they were one object, so running `a + b` reported b's failure on `a` -
        // anything bound to `a` alone lit up with an error it had nothing to do with.
        val first = RetryableAction("first", Icon.send, keepRunningWhile = scope) {}
        val second = RetryableAction("second", Icon.send, keepRunningWhile = scope) {
            throw IllegalStateException("second failed")
        }

        val combined = first.plus(second)
        combined.startAction(scope)

        assertEquals(null, first.state.exception, "the left operand succeeded and must report success")
        assertEquals("second failed", combined.state.exception?.message)

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun aDependencyChangingMidRunSettlesNeitherTheSubActionNorTheCombinedAction() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        val dependency = Signal(0)

        // Reading `dependency` inside the action registers it, so writing to it mid-run fires
        // onDependencyChange() while the coroutine is still parked on the gate. Neither action may
        // treat that as "finished, succeeded" - and the failure that follows must still surface.
        val first: Action = DependentAction("first", Icon.send, keepRunningWhile = scope) {
            dependency.await()
            gate.await()
            throw IllegalStateException("first failed")
        }
        val second: Action = DependentAction("second", Icon.send, keepRunningWhile = scope) {}

        val combined = first.plus(second)
        combined.startAction(scope)

        dependency.value = 1

        assertFalse(first.state.ready, "the dependency change reported the in-flight sub-action as finished")
        assertFalse(combined.state.ready, "combined action settled while a sub-action was still running")

        gate.complete(Unit)

        assertEquals(
            "first failed",
            combined.state.exception?.message,
            "combined action settled on the dependency change instead of waiting for the real run",
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun combinedActionStaysUnsettledUntilTheSlowerHalfFinishes() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()

        // A sub-action's state moves when it merely starts or finishes, not only when its own
        // dependencies change. A combined action that listens to those transitions reports done as
        // soon as the *first* half lands, with the second still parked.
        val first: Action = DependentAction("first", Icon.send, keepRunningWhile = scope) { firstGate.await() }
        val second: Action = DependentAction("second", Icon.send, keepRunningWhile = scope) { secondGate.await() }

        val combined = first.plus(second)
        combined.startAction(scope)

        firstGate.complete(Unit)

        assertTrue(first.state.success, "precondition: the first half really did finish")
        assertFalse(
            combined.state.ready,
            "combined action settled once the first half finished, with the second still running",
        )

        secondGate.complete(Unit)
        assertTrue(combined.state.success, "combined action never settled once both halves finished")

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun aRealFailureOutranksACancelledHalf() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()

        val first = RetryableAction("first", Icon.send, keepRunningWhile = scope) { firstGate.await() }
        val second = RetryableAction("second", Icon.send, keepRunningWhile = scope) {
            secondGate.await()
            throw IllegalStateException("second really failed")
        }

        val combined = first.plus(second)
        combined.startAction(scope)

        secondGate.complete(Unit)
        first.cancel()

        // Reporting the cancellation would discard a genuine error the user needs to see. It would
        // also leave the combined action stuck at "not ready" forever, since a CancellationException
        // cancels its coroutine instead of being recorded as a failure.
        assertEquals(
            "second really failed",
            combined.state.exception?.message,
            "the real failure was lost behind the cancelled half",
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun aDependencyChangeDoesNotClearTheErrorOfARunStillInFlight() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        val dependency = Signal(0)

        // The plain single-action case of the same rule: editing a form while its save is in flight
        // must not stop the spinner. Clearing is for the settled result a finished run left behind.
        val action: Action = DependentAction("save", Icon.send, keepRunningWhile = scope) {
            dependency.await()
            gate.await()
        }
        action.startAction(scope)

        dependency.value = 1

        assertFalse(action.state.ready, "a dependency change reported the in-flight run as finished")

        gate.complete(Unit)
        assertTrue(action.state.success)

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun combinedDependentActionClearsItsErrorWhenADependencyChanges() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val dependency = Signal(0)

        // Clearing the error on a dependency change is the entire difference between DependentAction
        // and RetryableAction, and a combined action has to keep it. Its own lambda reads no reactive
        // state, so it only tracks anything because `plus` registers the two sub-actions.
        val first: Action = DependentAction("first", Icon.send, keepRunningWhile = scope) {
            dependency.await()
            throw IllegalStateException("first failed")
        }
        val second: Action = DependentAction("second", Icon.send, keepRunningWhile = scope) {}

        val combined = first.plus(second)
        combined.startAction(scope)
        assertEquals("first failed", combined.state.exception?.message, "precondition: combined failed")

        dependency.value = 1

        assertEquals(
            null,
            combined.state.exception,
            "combined action kept a stale error after a dependency changed - a plain DependentAction would have cleared it",
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun cancellingASubActionsRunDoesNotLookLikeSuccess() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        var firstFinished = false

        val first = RetryableAction("first", Icon.send, keepRunningWhile = scope) {
            gate.await()
            firstFinished = true
        }
        val second = RetryableAction("second", Icon.send, keepRunningWhile = scope) {}

        val combined = first.plus(second)
        combined.startAction(scope)

        // Anything else driving `first` cancels the run the combined action is waiting on -
        // startAction() cancels lastJob before launching. Job.join() returns normally for a cancelled
        // job, so without an explicit check the combined action reports a clean success for work that
        // was thrown away half-done.
        first.cancel()

        assertFalse(firstFinished, "precondition: the cancelled run never reached its end")
        assertFalse(
            combined.state.success,
            "combined action reported success after a sub-action's run was cancelled",
        )

        scope.coroutineContext[Job]!!.cancel()
    }

    @Test
    fun combinedActionWaitsForAThirdPartyActionImplementation() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        var finished = false

        // Action is a public interface, so a sub-action need not be one of this file's classes and
        // need not expose a Job at all. Such an action has to be waited on through its reactive
        // state; treating "no job" as "nothing to wait for" would settle the combined action
        // immediately.
        val custom = object : Action {
            private val reportTo = RawReactive<Boolean>(ReactiveState.notReady)
            override val title: String get() = "custom"
            override val icon: Icon get() = Icon.send
            override val state: ReactiveState<Boolean> get() = reportTo.state
            override fun addListener(listener: () -> Unit): () -> Unit = reportTo.addListener(listener)
            override fun startAction(scope: CoroutineScope) {
                scope.launch {
                    gate.await()
                    finished = true
                    reportTo.state = ReactiveState(true)
                }
            }
            override fun plus(other: Action): Action = throw UnsupportedOperationException()
        }

        val first = RetryableAction("first", Icon.send, keepRunningWhile = scope) {}
        val combined = first.plus(custom)
        combined.startAction(scope)

        assertFalse(combined.state.ready, "combined action settled without waiting for the custom action")

        gate.complete(Unit)

        assertTrue(finished)
        assertTrue(combined.state.success, "combined action never settled once the custom action finished")

        scope.coroutineContext[Job]!!.cancel()
    }
}
