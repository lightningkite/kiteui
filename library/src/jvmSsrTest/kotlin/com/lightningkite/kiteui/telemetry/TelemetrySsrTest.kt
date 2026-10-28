package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.ssr.SsrContext
import kotlin.test.Test
import kotlin.test.assertNull

class TelemetrySsrTest {
    @Test
    fun installDoesNothingWhileServerRendering() {
        val telemetry = testTelemetry()
        val navigator = PageNavigator { error("no routes in this test") }
        // Once per request, as SSR runs app().
        repeat(2) { telemetry.install(SsrContext("/").elementContext, navigator) }
        assertNull(Telemetry.installed)
    }
}
