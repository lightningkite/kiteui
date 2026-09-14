package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.test.*

/** Stub pages for navigation tests. */
private class PageA : Page { override fun ElementWriter.CanAddTheme.render() {} }
private class PageB : Page { override fun ElementWriter.CanAddTheme.render() {} }
private class PageC : Page { override fun ElementWriter.CanAddTheme.render() {} }

class TracePerPageTest {

    private fun testConfig(traceSamplingRate: Double = 1.0) = TelemetryConfig(
        endpoint = "http://localhost:0/otlp",
        traceSamplingRate = traceSamplingRate,
    )

    private fun testNavigator() = PageNavigator { Routes(emptyList(), emptyMap()) }

    @Test
    fun firstNavigationKeepsSameTraceId() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()
        val initialTraceId = t.currentTraceId

        t.bindNavigation(nav)
        nav.reset(PageA())

        assertEquals(initialTraceId, t.currentTraceId,
            "First navigation should keep the initial trace ID (cold start trace)")
    }

    @Test
    fun secondNavigationRotatesTraceId() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)
        nav.reset(PageA())
        val traceAfterA = t.currentTraceId

        nav.navigate(PageB())
        val traceAfterB = t.currentTraceId

        assertNotEquals(traceAfterA, traceAfterB,
            "Navigating to a new page should create a new trace ID")
    }

    @Test
    fun eachNavigationGetsUniqueTraceId() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)

        val traceIds = mutableSetOf<String>()
        nav.reset(PageA())
        traceIds.add(t.currentTraceId)

        nav.navigate(PageB())
        traceIds.add(t.currentTraceId)

        nav.navigate(PageC())
        traceIds.add(t.currentTraceId)

        assertEquals(3, traceIds.size, "Each page should get a unique trace ID")
    }

    @Test
    fun pageSpanRecordedOnNavigation() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)
        nav.reset(PageA())

        // Navigate away to close PageA's span
        nav.navigate(PageB())

        val pageSpan = t.exporter.spanBuffer.find { it.name == "page: PageA" }
        assertNotNull(pageSpan, "Page span should be recorded when navigating away")
        assertEquals("PageA", pageSpan.attributes.find { it.key == "page.name" }?.value?.stringValue)
    }

    @Test
    fun pageSpanUsesCorrectTraceId() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)
        nav.reset(PageA())
        val traceForA = t.currentTraceId

        nav.navigate(PageB())

        val pageSpan = t.exporter.spanBuffer.find { it.name == "page: PageA" }
        assertNotNull(pageSpan)
        assertEquals(traceForA, pageSpan.traceId,
            "Page span should use the trace ID that was active during that page")
    }

    @Test
    fun currentSpanIdUpdatedOnNavigation() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)
        nav.reset(PageA())
        val spanA = t.currentSpanId
        assertTrue(spanA.isNotEmpty(), "currentSpanId should be set after navigation")

        nav.navigate(PageB())
        val spanB = t.currentSpanId
        assertNotEquals(spanA, spanB, "currentSpanId should change on navigation")
    }

    @Test
    fun navigatingToSamePageDoesNotRotate() {
        val t = Telemetry(testConfig())
        val nav = testNavigator()

        t.bindNavigation(nav)
        nav.reset(PageA())
        val traceAfterA = t.currentTraceId
        val spanAfterA = t.currentSpanId

        // Push another PageA instance — same class name, should not trigger rotation
        nav.navigate(PageA())

        assertEquals(traceAfterA, t.currentTraceId,
            "Same page class should not rotate trace ID")
        assertEquals(spanAfterA, t.currentSpanId,
            "Same page class should not create a new page span")
    }
}
