package com.lightningkite.kiteui.telemetry

import kotlin.coroutines.CoroutineContext

/** The currently installed [Telemetry] instance, set by [Telemetry.install]. */
internal var activeTelemetry: Telemetry? = null

/**
 * Carries trace context through the coroutine hierarchy.
 *
 * Installed automatically on page coroutine scopes by the navigation instrumentation.
 * Read by `connectivityFetch` to set `traceparent` headers and `parentSpanId` on HTTP spans.
 *
 * Falls back to the active [Telemetry] instance's current trace/span ID when not present
 * in the coroutine context (e.g. code running outside a page's scope).
 */
data class TelemetryContext(
    val traceId: String,
    val spanId: String,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TelemetryContext> = Key
    companion object Key : CoroutineContext.Key<TelemetryContext> {
        /**
         * Captures the current trace context from the calling coroutine.
         * Reads from an existing [TelemetryContext] in the coroutine hierarchy first,
         * falling back to the active [Telemetry] instance's current IDs.
         *
         * Usage:
         * ```kotlin
         * launch(TelemetryContext.current()) {
         *     // HTTP calls here inherit the parent's trace context
         * }
         * ```
         */
        suspend fun current(): TelemetryContext {
            val ctx = kotlin.coroutines.coroutineContext
            return TelemetryContext(
                traceId = ctx.traceId(),
                spanId = ctx.spanId(),
            )
        }
    }
}

/** Read trace context from the coroutine context, falling back to the active Telemetry instance. */
internal fun CoroutineContext.traceId(): String =
    this[TelemetryContext]?.traceId ?: activeTelemetry?.currentTraceId ?: ""

internal fun CoroutineContext.spanId(): String =
    this[TelemetryContext]?.spanId ?: activeTelemetry?.currentSpanId ?: ""
