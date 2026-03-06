// by Claude - coroutine context element for trace context propagation.
// Flows through structured concurrency so child coroutines (e.g. fetch calls inside
// page actions) inherit the correct trace/span context even across suspension points.
package com.lightningkite.kiteui.telemetry

import kotlin.coroutines.CoroutineContext

/**
 * Carries trace context through the coroutine hierarchy.
 *
 * Installed automatically on page coroutine scopes by the navigation instrumentation.
 * Read by `connectivityFetch` to set `traceparent` headers and `parentSpanId` on HTTP spans.
 *
 * Falls back to [Telemetry.currentTraceId]/[Telemetry.currentSpanId] when not present
 * in the coroutine context (e.g. code running outside a page's scope).
 */
data class TelemetryContext(
    val traceId: String,
    val spanId: String,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TelemetryContext> = Key
    companion object Key : CoroutineContext.Key<TelemetryContext>
}

/** Read trace context from the coroutine context, falling back to the global Telemetry singleton. */
internal fun CoroutineContext.traceId(): String =
    this[TelemetryContext]?.traceId ?: Telemetry.currentTraceId

internal fun CoroutineContext.spanId(): String =
    this[TelemetryContext]?.spanId ?: Telemetry.currentSpanId
