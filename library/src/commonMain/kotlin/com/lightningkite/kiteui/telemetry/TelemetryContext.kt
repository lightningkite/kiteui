package com.lightningkite.kiteui.telemetry

import kotlin.coroutines.CoroutineContext

/** Computes a human-readable view path on demand. Implemented by [RView][com.lightningkite.kiteui.views.RView]. */
fun interface ViewPathProvider {
    fun viewPath(): String
}

/**
 * Carries trace context through the coroutine hierarchy.
 *
 * Installed automatically on every view's coroutine scope (in `postSetup()`).
 * Also read by `instrumentFetch` to set `traceparent` headers and `parentSpanId`
 * on HTTP spans.
 */
class TelemetryContext(
    val traceId: String = "",
    val spanId: String = "",
    val viewPathProvider: ViewPathProvider? = null,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TelemetryContext> = Key
    companion object Key : CoroutineContext.Key<TelemetryContext> {
        /**
         * Captures the current trace context from the calling coroutine.
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
            val existing = ctx[TelemetryContext] ?: return TelemetryContext()
            return TelemetryContext(
                traceId = existing.traceId,
                spanId = existing.spanId,
                viewPathProvider = existing.viewPathProvider,
            )
        }
    }
}

internal fun CoroutineContext.traceId(): String =
    this[TelemetryContext]?.traceId ?: ""

internal fun CoroutineContext.spanId(): String =
    this[TelemetryContext]?.spanId ?: ""

internal fun CoroutineContext.viewPath(): String =
    this[TelemetryContext]?.viewPathProvider?.viewPath() ?: ""
