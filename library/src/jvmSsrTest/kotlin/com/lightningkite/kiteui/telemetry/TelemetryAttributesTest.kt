package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.elementTree
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.LoadTracker
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Records carry the current screen, the app's attributes and their context's, as they are when recorded. */
class TelemetryAttributesTest {
    private val flavor = TelemetryKey.OfString("app.flavor")
    private val root = ElementContext("/")

    private object HomePage : Page {
        override fun ElementWriter.CanAddTheme.render() {}
    }

    private object SettingsPage : Page {
        override fun ElementWriter.CanAddTheme.render() {}
    }

    @Test
    fun attributesSetAfterAnElementIsBuiltAreOnItsNextSpan() = runTest {
        val t = testTelemetry()
        val tree = elementTree { text { debugName = "leaf" } }
        try {
            val leaf = tree.findByName("leaf")!!.underlyingNativeElement
            tree.context.telemetryAttributes = telemetryAttributesOf(flavor to "gamma")
            withContext(leaf.coroutineContext[ElementContext]!!) { t.span("checkout", "pay") {} }
        } finally {
            tree.shutdown()
        }
        assertEquals("gamma", t.recordedSpans().single().attributes[flavor])
    }

    @Test
    fun aContainersAttributesAddToTheRootsWithoutCopyingThem() = runTest {
        val t = testTelemetry()
        val step = TelemetryKey.OfString("checkout.step")
        val section = root.split().apply { telemetryAttributes += telemetryAttributesOf(step to "card") }
        val override = section.split().apply { telemetryAttributes += telemetryAttributesOf(flavor to "local") }
        root.telemetryAttributes = telemetryAttributesOf(flavor to "gamma")
        withContext(section) { t.span("checkout", "pay") {} }
        withContext(override) { t.span("checkout", "confirm") {} }

        val (pay, confirm) = t.recordedSpans().map { it.attributes }
        assertEquals("gamma", pay[flavor], "the root's change reaches the container")
        assertEquals("card", pay[step])
        assertEquals(telemetryAttributesOf(step to "card"), section.telemetryAttributes, "only its own")
        assertEquals("local", confirm[flavor], "nearer values win")
        assertEquals("card", confirm[step])
    }

    @Test
    fun everythingCarriesTheCurrentScreenAndTheAppsAttributes() = runTest {
        val t = testTelemetry()
        t.currentScreen = telemetryAttributesOf(TelemetryKeys.App.screenName to "HomePage")
        t.attributes = telemetryAttributesOf(flavor to "beta")
        withContext(root.split()) { t.span("dialog", "confirm") {} }
        t.reportError(IllegalStateException("boom"), TelemetryAttributes.empty)

        val records = t.recordedSpans().map { it.attributes } + t.recordedLogs().map { it.attributes }
        assertEquals(listOf("HomePage", "HomePage"), records.map { it[TelemetryKeys.App.screenName] })
        assertEquals(listOf("beta", "beta"), records.map { it[flavor] })
    }

    @Test
    fun actionCarriesTheAttributesOfTheScopeItStartedFrom() = runTest {
        val t = testTelemetry()
        val section = root.split().apply { telemetryAttributes = telemetryAttributesOf(flavor to "beta") }
        // Runs here, outside the element's scope, as an action with keepRunningWhile = AppScope would.
        runCatching {
            t.actionInterceptor.intercept(CoroutineScope(section + Job()), "Save") {
                t.instrumentFetch("https://example.com/save", HttpMethod.POST, HttpHeaders(), null) { _, _, _, _ -> error("offline") }
            }
        }

        assertEquals(listOf("beta", "beta"), t.recordedSpans().map { it.attributes[flavor] })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun eachPageShownIsAVisitThatLoadsWhenNothingIsLoading() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val t = testTelemetry()
            val load = LoadTracker(outside = null)
            load.startedLoading()
            t.onPageShown(HomePage, load)
            val home = t.currentScreen
            assertTrue(t.recordedSpans().isEmpty(), "still loading")
            load.finishedLoading()
            t.onPageShown(SettingsPage, load)

            assertEquals("HomePage", home[TelemetryKeys.App.screenName])
            assertEquals("SettingsPage", t.currentScreen[TelemetryKeys.App.screenName])
            assertNotEquals(home[TelemetryKeys.App.screenId], t.currentScreen[TelemetryKeys.App.screenId])
            assertEquals(listOf("HomePage", "SettingsPage"), t.recordedSpans().map { it.attributes[TelemetryKeys.App.screenName] })
            val end = t.recordedLogs().single { it.message == "app.screen.view_end" }
            assertEquals("HomePage", end.attributes[TelemetryKeys.App.screenName], "the visit ends labelled with its own screen")
        } finally {
            Dispatchers.resetMain()
        }
    }
}
