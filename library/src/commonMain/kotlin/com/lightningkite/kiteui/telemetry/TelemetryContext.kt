package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.viewPath
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
    val element: NativeElement? = null,
    val sampled: Boolean = false,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TelemetryContext> get() = Key
    companion object Key : CoroutineContext.Key<TelemetryContext>
}

internal fun CoroutineContext.traceId(): String =
    this[TelemetryContext]?.traceId ?: ""

internal fun CoroutineContext.spanId(): String =
    this[TelemetryContext]?.spanId ?: ""

internal fun CoroutineContext.viewPath(): String =
    this[TelemetryContext]?.element?.outermostElement?.viewPath() ?: ""
