package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TelemetryTest {
    private class Offline : Exception()
    private val navigator = PageNavigator { error("no routes in this test") }
    private val screenAttributes = telemetryAttributesOf(
        TelemetryKeys.App.screenName to "HomePage",
        TelemetryKeys.App.screenId to "abc",
    )

    @Test
    fun spanWithoutParentStartsATrace() = runTest {
        val t = testTelemetry()
        t.span("checkout", "first") {}
        t.span("checkout", "second") {}

        val (first, second) = t.recordedSpans()
        assertNull(first.parentSpanId)
        assertNotEquals(first.traceId, second.traceId)
        assertEquals("checkout", first.owner)
        assertEquals("first", first.operation)
        assertEquals(RecordedSpan.Outcome.Ok, first.outcome)
    }

    @Test
    fun nestedSpanIsAChild() = runTest {
        val t = testTelemetry()
        t.span("checkout", "outer") { t.span("checkout", "inner") {} }

        val inner = t.recordedSpans().single { it.operation == "inner" }
        val outer = t.recordedSpans().single { it.operation == "outer" }
        assertEquals(outer.traceId, inner.traceId)
        assertEquals(outer.spanId, inner.parentSpanId)
    }

    @Test
    fun spanCarriesAmbientAndItsOwnAttributes() = runTest {
        val t = testTelemetry()
        val step = TelemetryKey.OfString("checkout.step")
        withContext(TelemetryAttributesElement(screenAttributes)) {
            t.span("checkout", "pay", telemetryAttributesOf(step to "card")) {}
        }

        val span = t.recordedSpans().single()
        assertEquals("HomePage", span.attributes[TelemetryKeys.App.screenName])
        assertEquals("card", span.attributes[step])
    }

    @Test
    fun failedSpanRecordsErrorWithFingerprint() = runTest {
        val t = testTelemetry()
        assertFailsWith<IllegalStateException> { t.span("checkout", "pay") { error("declined") } }

        val span = t.recordedSpans().single()
        assertEquals(RecordedSpan.Outcome.Error, span.outcome)
        assertEquals("declined", span.error?.message)
        assertTrue(span.error!!.fingerprint.isNotEmpty())
    }

    @Test
    fun cancelledSpanIsRecordedAsCancelled() = runTest {
        val t = testTelemetry()
        val job = launch { t.span("screen", "load") { kotlinx.coroutines.awaitCancellation() } }
        yield()
        job.cancel()
        job.join()

        assertEquals(RecordedSpan.Outcome.Cancelled, t.recordedSpans().single().outcome)
    }

    @Test
    fun unsampledSessionRecordsNoSpans() = runTest {
        val t = testTelemetry(traceSamplingRate = 0.0)
        t.span("checkout", "pay") {}
        assertTrue(t.recordedSpans().isEmpty())
    }

    @Test
    fun reportErrorRecordsAnErrorLogWithFingerprint() {
        val t = testTelemetry()
        t.currentScreen = screenAttributes
        t.reportError(IllegalStateException("boom"), telemetryAttributesOf(TelemetryKey.OfString("where") to "here"))

        val log = t.recordedLogs().single()
        assertEquals("boom", log.message)
        assertEquals("IllegalStateException", log.error?.type)
        assertTrue(log.error!!.fingerprint.isNotEmpty())
        assertEquals("HomePage", log.attributes[TelemetryKeys.App.screenName])
        assertEquals("here", log.attributes[TelemetryKey.OfString("where")])
    }

    @Test
    fun appSpansGoToTheInstalledTelemetry() = runTest {
        assertEquals(1, span("checkout", "pay") { 1 }, "without telemetry the action just runs")

        val t = testTelemetry()
        Telemetry.installed = t
        try {
            span("checkout", "pay") {}
        } finally {
            Telemetry.installed = null
        }
        assertEquals("pay", t.recordedSpans().single().operation)
    }

    @Test
    fun spanOwnersMustBeValidNames() = runTest {
        assertFailsWith<IllegalArgumentException> { span("check out", "pay") {} }
    }

    @Test
    fun actionCarriesTheScreenAttributesOfTheScopeItStartedFrom() = runTest {
        val t = testTelemetry()
        val elementScope = CoroutineScope(TelemetryAttributesElement(screenAttributes) + Job())
        // Runs here, outside elementScope, as an action with keepRunningWhile = AppScope would.
        t.actionInterceptor.intercept(elementScope, "Save") {
            try {
                t.instrumentFetch("https://example.com/save", HttpMethod.POST, HttpHeaders(), null) { _, _, _, _ -> throw Offline() }
            } catch (_: Offline) {}
        }

        val action = t.recordedSpans().single { it.owner == "action" }
        val request = t.recordedSpans().single { it.owner == "http" }
        assertEquals("Save", action.operation)
        assertEquals("HomePage", action.attributes[TelemetryKeys.App.screenName])
        assertEquals(action.spanId, request.parentSpanId, "a request inside an action is its child")
        assertEquals(action.traceId, request.traceId)
    }

    @Test
    fun screenLoadEndsWhenTheScreenIsUsable() = runTest {
        val t = testTelemetry()
        val load = ScreenLoad()
        load.startedLoading()
        val visit = launch(TelemetryAttributesElement(screenAttributes)) { t.recordScreenVisit(navigator, load) }
        load.finishedRendering()
        yield()
        assertTrue(t.recordedSpans().isEmpty(), "still loading")

        load.finishedLoading()
        yield()
        val span = t.recordedSpans().single()
        assertEquals("screen", span.owner)
        assertEquals("load", span.operation)
        assertEquals(RecordedSpan.Outcome.Ok, span.outcome)
        assertEquals("HomePage", span.attributes[TelemetryKeys.App.screenName])
        visit.cancel()
    }

    @Test
    fun aScreenThatNeverBecomesUsableIsAnErrorAfterSixtySeconds() = runTest {
        val t = testTelemetry()
        val load = ScreenLoad()
        load.startedLoading()
        val visit = launch(TelemetryAttributesElement(screenAttributes)) { t.recordScreenVisit(navigator, load) }
        advanceTimeBy(61_000)

        assertEquals(RecordedSpan.Outcome.Error, t.recordedSpans().single().outcome)
        assertTrue(visit.isActive, "the visit continues after the load gives up")
        visit.cancel()
    }

    @Test
    fun leavingBeforeTheScreenIsUsableCancelsTheLoad() = runTest {
        val t = testTelemetry()
        val load = ScreenLoad()
        load.startedLoading()
        val visit = launch(TelemetryAttributesElement(screenAttributes)) { t.recordScreenVisit(navigator, load) }
        yield()
        visit.cancel()
        visit.join()

        assertEquals(RecordedSpan.Outcome.Cancelled, t.recordedSpans().single().outcome)
        val events = t.recordedLogs().map { it.message }
        assertEquals(listOf("app.screen.view_start", "app.screen.view_end"), events)
    }
}
