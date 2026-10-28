package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.RetryableAction
import com.lightningkite.kiteui.testing.BaseUiTest
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Claude: a BaseUiTest because Icon needs Android resources.
class ActionTelemetryTest : BaseUiTest() {
    private val screenAttributes = telemetryAttributesOf(TelemetryKeys.App.screenName to "HomePage")

    @Test
    fun workLaunchedInAnActionAndTheHalvesOfACombinedActionAreChildren() {
        val t = testTelemetry()
        Telemetry.installed = t
        val release = Action.Interceptor.global.register(t.actionInterceptor)
        try {
            // Claude: unconfined, so the actions run to completion inside startAction.
            val appScope = CoroutineScope(Job() + Dispatchers.Unconfined)
            val elementScope = CoroutineScope(TelemetryAttributesElement(screenAttributes) + Job() + Dispatchers.Unconfined)
            val first = RetryableAction("First", Icon.send, keepRunningWhile = appScope) { async { span("work", "launched") {} }.await() }
            val second = RetryableAction("Second", Icon.send, keepRunningWhile = appScope) {}
            (first + second).startAction(elementScope)
        } finally {
            release()
            Telemetry.installed = null
        }

        val spans = t.recordedSpans()
        val combined = spans.single { it.owner == "action" && it.parentSpanId == null }
        val halves = spans.filter { it.owner == "action" && it !== combined }
        val launched = spans.single { it.operation == "launched" }
        assertEquals(listOf("First", "Second"), halves.map { it.operation }.sorted())
        assertTrue(halves.all { it.parentSpanId == combined.spanId }, "each half is a child of the combined action")
        assertEquals(halves.single { it.operation == "First" }.spanId, launched.parentSpanId, "work launched in an action is its child")
        assertTrue(spans.all { it.traceId == combined.traceId }, "one trace")
        assertTrue(spans.all { it.attributes[TelemetryKeys.App.screenName] == "HomePage" }, "all carry the screen")
    }
}
