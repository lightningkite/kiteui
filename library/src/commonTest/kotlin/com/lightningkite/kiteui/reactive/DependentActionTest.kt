package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.context.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DependentActionTest {

    /**
     * Regression test: a [DependentAction]'s error state must be cleared when one of the reactive
     * dependencies read inside the action changes. This only works if the action coroutine is
     * launched with the [DependentAction] itself in its coroutine context, so that the reactive
     * `invoke()`/`await()` calls can find it and register their dependencies against it.
     */
    @Test
    fun dependencyChangeClearsError() {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val trigger = Signal(0)

        val action = DependentAction(
            title = "test",
            icon = Icon.send,
            keepRunningWhile = scope,
            action = {
                // Reading the dependency registers it against the action.
                trigger()
                throw IllegalStateException("boom")
            },
        )

        // Run the action; under Unconfined it executes synchronously to completion.
        action.startAction(scope)
        assertNotNull(action.state.exception, "action should be in an error state after throwing")

        // Changing a dependency read during the action must clear the error.
        trigger.value = 1
        assertNull(action.state.exception, "dependency change should have cleared the error state")
        assertEquals(false, action.state.getOrNull(), "state should reset to a non-loading, non-error false")

        scope.coroutineContext[Job]!!.cancel()
    }
}
