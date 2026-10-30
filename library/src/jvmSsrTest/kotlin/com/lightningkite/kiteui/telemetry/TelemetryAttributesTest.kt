package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.elementTree
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementContextElement
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

/** Records carry the screen they were made on and the context's [telemetryAttributes] as they are when recorded. */
class TelemetryAttributesTest {
    private val flavor = TelemetryKey.OfString("app.flavor")
    private val root = ElementContext("/").apply { telemetryAttributes = telemetryAttributesOf(flavor to "beta") }
    private val screen = root.split().apply { screenVisit = ScreenVisit("HomePage") }

    @Test
    fun attributesSetAfterAnElementIsBuiltAreOnItsNextSpan() = runTest {
        val t = testTelemetry()
        val tree = elementTree { text { debugName = "leaf" } }
        try {
            val leaf = tree.findByName("leaf")!!.underlyingNativeElement
            tree.context.telemetryAttributes = telemetryAttributesOf(flavor to "gamma")
            withContext(leaf.coroutineContext[ElementContextElement]!!) { t.span("checkout", "pay") {} }
        } finally {
            tree.shutdown()
        }
        assertEquals("gamma", t.recordedSpans().single().attributes[flavor])
    }

    @Test
    fun aContainersAttributesAddToTheRootsWithoutCopyingThem() = runTest {
        val t = testTelemetry()
        val step = TelemetryKey.OfString("checkout.step")
        val section = screen.split().apply { telemetryAttributes += telemetryAttributesOf(step to "card") }
        val override = section.split().apply { telemetryAttributes += telemetryAttributesOf(flavor to "local") }
        root.telemetryAttributes = telemetryAttributesOf(flavor to "gamma")
        withContext(ElementContextElement(section)) { t.span("checkout", "pay") {} }
        withContext(ElementContextElement(override)) { t.span("checkout", "confirm") {} }

        val (pay, confirm) = t.recordedSpans().map { it.attributes }
        assertEquals("gamma", pay[flavor], "the root's change reaches the container")
        assertEquals("card", pay[step])
        assertEquals(telemetryAttributesOf(step to "card"), section.telemetryAttributes, "only its own")
        assertEquals("local", confirm[flavor], "nearer values win")
        assertEquals("card", confirm[step])
    }

    @Test
    fun spansCarryTheirScreenAndTheAppsAttributes() = runTest {
        val t = testTelemetry()
        withContext(ElementContextElement(screen)) { t.span("checkout", "pay") {} }

        val span = t.recordedSpans().single()
        assertEquals("HomePage", span.attributes[TelemetryKeys.App.screenName])
        assertEquals("beta", span.attributes[flavor])
    }

    @Test
    fun recordsOutsideAnyScreenCarryTheCurrentScreen() = runTest {
        val t = testTelemetry()
        t.currentScreen = screen
        withContext(ElementContextElement(root.split())) { t.span("dialog", "confirm") {} } // a dialog, say
        t.reportError(IllegalStateException("boom"), TelemetryAttributes.empty)

        val records = t.recordedSpans().map { it.attributes } + t.recordedLogs().map { it.attributes }
        assertEquals(listOf("HomePage", "HomePage"), records.map { it[TelemetryKeys.App.screenName] })
        assertEquals(listOf("beta", "beta"), records.map { it[flavor] })
    }

    @Test
    fun actionCarriesTheAttributesOfTheScopeItStartedFrom() = runTest {
        val t = testTelemetry()
        // Runs here, outside the element's scope, as an action with keepRunningWhile = AppScope would.
        runCatching {
            t.actionInterceptor.intercept(CoroutineScope(ElementContextElement(screen) + Job()), "Save") {
                t.instrumentFetch("https://example.com/save", HttpMethod.POST, HttpHeaders(), null) { _, _, _, _ -> error("offline") }
            }
        }

        assertEquals(listOf("HomePage", "HomePage"), t.recordedSpans().map { it.attributes[TelemetryKeys.App.screenName] })
    }
}
