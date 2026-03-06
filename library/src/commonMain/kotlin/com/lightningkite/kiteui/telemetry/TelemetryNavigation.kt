// by Claude - navigation span tracking for OpenTelemetry.
// Records page view spans (time on page) and navigation.page_views counter.
package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator

internal object TelemetryNavigation {
    private var cleanup: (() -> Unit)? = null
    private var lastPageName: String? = null
    private var lastPageStartNanos: String = ""
    private var lastPageSpanId: String = ""

    /**
     * Bind navigation tracking to a [PageNavigator].
     * Called automatically from `appBase()` when telemetry is active.
     */
    fun bind(navigator: PageNavigator) {
        cleanup?.invoke()
        cleanup = navigator.stack.addListener {
            val page = navigator.stack.value.lastOrNull()
            val pageName = page?.let { it::class.simpleName } ?: "empty"

            if (pageName != lastPageName) {
                endCurrentPageSpan()

                lastPageName = pageName
                lastPageStartNanos = IdGenerator.nanosString()
                lastPageSpanId = IdGenerator.spanId()
                Telemetry.currentSpanId = lastPageSpanId

                // Record page view counter (always, not sampled — cheap aggregation)
                Telemetry.exporter?.incrementCounter(
                    name = "navigation.page_views",
                    attributes = listOf(
                        OtlpKeyValue("page.name", OtlpAnyValue(stringValue = pageName)),
                    )
                )
            }
        }
    }

    private fun endCurrentPageSpan() {
        val name = lastPageName ?: return
        val config = Telemetry.config ?: return
        if (kotlin.random.Random.nextDouble() > config.traceSamplingRate) return

        Telemetry.exporter?.addSpan(
            OtlpSpan(
                traceId = Telemetry.currentTraceId,
                spanId = lastPageSpanId,
                name = "page: $name",
                kind = 1, // SPAN_KIND_INTERNAL
                startTimeUnixNano = lastPageStartNanos,
                endTimeUnixNano = IdGenerator.nanosString(),
                attributes = listOf(
                    OtlpKeyValue("page.name", OtlpAnyValue(stringValue = name)),
                    OtlpKeyValue("session.id", OtlpAnyValue(stringValue = Telemetry.sessionId)),
                ),
            )
        )
    }

    fun unbind() {
        endCurrentPageSpan()
        cleanup?.invoke()
        cleanup = null
        lastPageName = null
    }
}
