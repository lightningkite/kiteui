package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.views.Element
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext


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
    val element: (() -> Element)? = null,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TelemetryContext> get() = Key

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
            val ctx = currentCoroutineContext()
            val existing = ctx[TelemetryContext] ?: return TelemetryContext()
            return TelemetryContext(
                traceId = existing.traceId,
                spanId = existing.spanId,
                element = existing.element,
            )
        }
    }
}

internal fun CoroutineContext.traceId(): String =
    this[TelemetryContext]?.traceId ?: ""

internal fun CoroutineContext.spanId(): String =
    this[TelemetryContext]?.spanId ?: ""

internal fun CoroutineContext.viewPath(): String =
    this[TelemetryContext]?.element?.viewPath() ?: ""
