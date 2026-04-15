package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Hook for recording spans. Set by [Telemetry.install], cleared by [Telemetry.shutdown].
 * Used by the public [span] function.
 */
internal var spanRecorder: ((OtlpSpan) -> Unit)? = null

/**
 * The most specific active trace/span IDs on the main thread.
 * Updated by [span] and by the action instrumentor on start/end.
 * Read by [TelemetryLogInterceptor] and [Telemetry.recordException] for log/exception attribution.
 *
 * These are best-effort for interleaved coroutines — correct when actions don't overlap,
 * which is the common case (actions default to [ignoreRetryWhileRunning] = true).
 */
internal var activeTraceId: String = ""
internal var activeSpanId: String = ""

/**
 * Creates a child span around [block], propagating trace context so that
 * nested [span] calls and HTTP requests become children of this span.
 *
 * Usage:
 * ```kotlin
 * button {
 *     onClick("Save") {
 *         span("validate") { validate(form) }
 *         span("upload-file") { uploadFile(attachment) }
 *         span("save-record") { api.put("/records", record) }
 *     }
 * }
 * ```
 *
 * If telemetry is not installed or the current trace is not sampled,
 * the block executes normally with no overhead beyond a context check.
 */
suspend fun <T> span(
    name: String,
    attributes: List<OtlpKeyValue> = emptyList(),
    block: suspend () -> T,
): T {
    val recorder = spanRecorder ?: return block()
    val parentCtx = coroutineContext[TelemetryContext] ?: return block()
    val traceId = parentCtx.traceId.ifEmpty { return block() }

    val childSpanId = Telemetry.spanId()
    val childCtx = TelemetryContext(
        traceId = traceId,
        spanId = childSpanId,
        viewPathProvider = parentCtx.viewPathProvider,
        sampled = parentCtx.sampled,
    )

    val savedTraceId = activeTraceId
    val savedSpanId = activeSpanId
    activeTraceId = traceId
    activeSpanId = childSpanId

    val startNanos = Telemetry.nanosString()
    var error: Throwable? = null
    try {
        return withContext(childCtx) { block() }
    } catch (e: Throwable) {
        error = e
        throw e
    } finally {
        activeTraceId = savedTraceId
        activeSpanId = savedSpanId
        if (parentCtx.sampled) {
            recorder(
                OtlpSpan(
                    traceId = traceId,
                    spanId = childSpanId,
                    parentSpanId = parentCtx.spanId,
                    name = name,
                    startTimeUnixNano = startNanos,
                    endTimeUnixNano = Telemetry.nanosString(),
                    attributes = attributes,
                    status = error?.let {
                        OtlpSpanStatus(code = 2, message = it.message ?: it::class.simpleName ?: "Error")
                    },
                )
            )
        }
    }
}
