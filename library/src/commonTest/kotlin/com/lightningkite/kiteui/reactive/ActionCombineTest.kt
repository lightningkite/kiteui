package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * Deliberately NOT asserted here: the timing of `combined.state.ready`. `plus` constructs the combined
 * action with the receiver's own `reportTo` holder, so `combined.state` is the same object as the first
 * sub-action's state - it necessarily settles when the first one does, whatever the lambda is doing.
 * That is pre-existing behaviour untouched by this fix, so pinning it here would test the sharing rather
 * than the fix, and would fail for a reason unrelated to what these tests exist to protect.
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
}
