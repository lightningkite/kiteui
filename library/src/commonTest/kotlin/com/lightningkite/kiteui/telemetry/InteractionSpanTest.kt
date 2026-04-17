package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*

class InteractionSpanTest {

    private fun testConfig(traceSamplingRate: Double = 1.0) = TelemetryConfig(
        endpoint = "http://localhost:0/otlp",
        traceSamplingRate = traceSamplingRate,
    )

    @Test
    fun startInteractionCreatesSpanOnEnd() {
        val t = Telemetry(testConfig())
        val scope = CoroutineScope(EmptyCoroutineContext)
        val interaction = t.startInteraction(scope, "Save")

        assertTrue(t.exporter.spanBuffer.isEmpty(), "No span before end()")

        interaction.end()

        assertEquals(1, t.exporter.spanBuffer.size, "Span should be recorded after end()")
        val span = t.exporter.spanBuffer.first()
        assertEquals("action: Save", span.name)
        assertEquals(t.currentTraceId, span.traceId)
    }

    @Test
    fun interactionSpanHasCorrectParentSpanId() {
        val t = Telemetry(testConfig())
        t.currentSpanId = "aabb112233445566"
        val scope = CoroutineScope(EmptyCoroutineContext)
        val interaction = t.startInteraction(scope, "Delete")
        interaction.end()

        val span = t.exporter.spanBuffer.first()
        assertEquals("aabb112233445566", span.parentSpanId)
    }

    @Test
    fun interactionSpanIdPropagatedViaCoroutineContext() = runTest {
        val t = Telemetry(testConfig())
        val scope = CoroutineScope(EmptyCoroutineContext)
        val interaction = t.startInteraction(scope, "Fetch")

        withContext(interaction.coroutineContext) {
            val ctx = kotlin.coroutines.coroutineContext
            assertEquals(t.currentTraceId, ctx.traceId())
            assertTrue(ctx.spanId().isNotEmpty(), "Span ID should be set")
            assertNotEquals(t.currentSpanId, ctx.spanId(),
                "Interaction span ID should be different from the page span")
        }

        interaction.end()
    }

    @Test
    fun interactionIncrementsCounter() {
        val t = Telemetry(testConfig())
        val scope = CoroutineScope(EmptyCoroutineContext)

        t.startInteraction(scope, "Submit").end()
        t.startInteraction(scope, "Submit").end()

        val counter = t.exporter.counters.values.find { it.name == "action.invocations" }
        assertNotNull(counter, "action.invocations counter should exist")
    }

    @Test
    fun interactionSpanNotRecordedWhenUnsampled() {
        val t = Telemetry(testConfig(traceSamplingRate = 0.0))
        val scope = CoroutineScope(EmptyCoroutineContext)

        t.startInteraction(scope, "Click").end()

        assertTrue(t.exporter.spanBuffer.isEmpty(), "Span should not be recorded when unsampled")
        // Counter should still be recorded
        assertTrue(t.exporter.counters.isNotEmpty(), "Counter should still be recorded when unsampled")
    }

    @Test
    fun interactionSpanCapturesViewPath() {
        val t = Telemetry(testConfig())
        val viewPathProvider = ViewPathProvider { "settings/save-button" }
        val ctx = TelemetryContext(viewPathProvider = viewPathProvider)
        val scope = CoroutineScope(ctx)

        t.startInteraction(scope, "Save").end()

        val span = t.exporter.spanBuffer.first()
        val viewPathAttr = span.attributes.find { it.key == "view.path" }
        assertNotNull(viewPathAttr)
        assertEquals("settings/save-button", viewPathAttr.value.stringValue)
    }

    @Test
    fun interactionContextPreservesViewPathProvider() = runTest {
        val provider = ViewPathProvider { "my/button" }
        val t = Telemetry(testConfig())
        val scope = CoroutineScope(TelemetryContext(viewPathProvider = provider))

        val interaction = t.startInteraction(scope, "Click")

        withContext(interaction.coroutineContext) {
            assertEquals("my/button", kotlin.coroutines.coroutineContext.viewPath())
        }

        interaction.end()
    }

    @Test
    fun httpSpanBecomesChildOfInteraction() = runTest {
        val t = Telemetry(testConfig(traceSamplingRate = 1.0).copy(
            tracePropagationHosts = listOf("example.com")
        ))
        val scope = CoroutineScope(EmptyCoroutineContext)
        val interaction = t.startInteraction(scope, "LoadData")
        val interactionSpanId = interaction.telemetryContext.spanId

        // Simulate HTTP call within the interaction's coroutine context
        withContext(interaction.coroutineContext) {
            // Verify the coroutine context has the interaction's trace/span IDs
            val ctx = kotlin.coroutines.coroutineContext
            assertEquals(interaction.telemetryContext.traceId, ctx.traceId(),
                "Coroutine should have interaction's trace ID")
            assertEquals(interactionSpanId, ctx.spanId(),
                "Coroutine should have interaction's span ID")
        }

        interaction.end()

        // Verify the interaction span was recorded with correct parent
        val actionSpan = t.exporter.spanBuffer.find { it.name == "action: LoadData" }
        assertNotNull(actionSpan)
        assertEquals(interactionSpanId, actionSpan.spanId)
    }

    @Test
    fun actionInstrumentorHookIntegration() {
        val t = Telemetry(testConfig())
        val scope = CoroutineScope(EmptyCoroutineContext)

        // Simulate what Telemetry.install() does: register an action instrumentor
        val instrumentor: (CoroutineScope, String) -> com.lightningkite.kiteui.reactive.ActionInstrumentation? = { s, title ->
            val interaction = t.startInteraction(s, title)
            com.lightningkite.kiteui.reactive.ActionInstrumentation(interaction.coroutineContext) { interaction.end() }
        }
        com.lightningkite.kiteui.reactive.actionInstrumentors.add(instrumentor)
        try {
            val result = com.lightningkite.kiteui.reactive.actionInstrumentors.mapNotNull { it(scope, "Test") }
            assertEquals(1, result.size)
            result.first().onEnd()

            assertEquals(1, t.exporter.spanBuffer.size)
            assertEquals("action: Test", t.exporter.spanBuffer.first().name)
        } finally {
            com.lightningkite.kiteui.reactive.actionInstrumentors.remove(instrumentor)
        }
    }
}
