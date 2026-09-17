package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.ActionInstrumentation
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.actionInstrumentors
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.viewPath
import com.lightningkite.reactive.core.AppScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * KiteUI's built-in OpenTelemetry integration.
 *
 * Usage:
 * ```kotlin
 * val telemetry = Telemetry(TelemetryConfig(
 *     endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp",
 *     headers = mapOf("Authorization" to "Basic $base64Token")
 * ))
 * telemetry.install()
 * ```
 *
 * When installed, the following are auto-instrumented:
 * - HTTP request spans and latency histograms
 * - Page navigation spans and view counters
 * - App lifecycle (cold start, foreground sessions)
 * - Connectivity issues
 * - Warn/Error log records and exception reports
 */
public class Telemetry(public val config: TelemetryConfig) {

    // ===== Instance state =====

    internal val sessionId: String = spanId()
    internal var currentTraceId: String = traceId()
        internal set
    internal var currentSpanId: String = ""
        internal set

    internal val exporter: TelemetryExporter = TelemetryExporter(config)

    /** Sampling decision made once per session (app launch). All traces in this session share the same decision. */
    internal val currentTraceIsSampled: Boolean = Random.nextDouble() < config.traceSamplingRate

    /** Effective log severity — mutable to support [setVerboseLogging]. */
    internal var logMinSeverity: OtlpSeverity = config.logMinSeverity
        private set

    private val log = LogRoot.tag("Telemetry")
    private val coldStartNanos = nanosString()
    private var lastForegroundNanos: String = ""
    private var foregroundSpanId: String = ""

    // Navigation tracking
    private var navCleanup: (() -> Unit)? = null
    private var lastPageName: String? = null
    private var lastPageStartNanos: String = ""
    private var lastPageSpanId: String = ""

    // Cleanup tracking for shutdown() — flags deactivate hooks without un-registering,
    // which avoids breaking the delegation chain or leaking references.
    private var installed = false
    private var throwableHookActive = false
    private var crashHookActive = false
    private val installedFetchInterceptor: FetchInterceptor = { url, method, headers, body, proceed ->
        instrumentFetch(url, method, headers, body, proceed)
    }
    private val installedLogInterceptor = TelemetryLogInterceptor(this)
    private val installedActionInstrumentor: (CoroutineScope, String) -> ActionInstrumentation? = { scope, title ->
        val interaction = startInteraction(scope, title)
        var savedTraceId = ""
        var savedSpanId = ""
        ActionInstrumentation(
            coroutineContext = interaction.coroutineContext,
            onStart = {
                savedTraceId = activeTraceId
                savedSpanId = activeSpanId
                activeTraceId = interaction.telemetryContext.traceId
                activeSpanId = interaction.telemetryContext.spanId
            },
            onEnd = {
                activeTraceId = savedTraceId
                activeSpanId = savedSpanId
                interaction.end()
            },
        )
    }
    private val installedSpanRecorder: (OtlpSpan) -> Unit = { exporter.addSpan(it) }
    private val exceptionHandlerInterceptor = ExceptionHandler(10f) { e, meta ->
        recordException(e,
            if (meta == null) "" else listOfNotNull(
                meta.source?.let { "$it @ ${it.viewPath()}" },
                meta.process?.let {
                    val s = when (meta.foregroundProcess) {
                        true -> " (f)"
                        false -> " (b)"
                        null -> ""
                    }
                    "p$s: $it"
                }
            ).joinToString(" ")
        )

        return@ExceptionHandler null    // intercept, don't capture
    }
    private val cleanups = mutableListOf<() -> Unit>()
    private var elementContext: ElementContext? = null

    // ===== Installation =====

    /**
     * Installs all global hooks (log interceptor, exception capture, fetch instrumentation,
     * navigation binding, lifecycle tracking). Call after construction.
     *
     * Separated from construction so tests can create instances without triggering
     * AppScope/lifecycle hooks.
     */
    public fun install(context: ElementContext, navigator: PageNavigator) {
        check(!installed) { "Telemetry.install() called twice. Call shutdown() first." }
        installed = true

        // Log interceptor
        Log.interceptors.add(installedLogInterceptor)

        // Exception handler
        elementContext = context
        context.exceptionHandlers += exceptionHandlerInterceptor

        // Exception capture — uses a flag so shutdown() never breaks the delegation chain.
        // A save/restore pattern would drop hooks installed by other code between install() and shutdown().
        throwableHookActive = true
        val previous = Throwable_report
        Throwable_report = { throwable, context ->
            previous(throwable, context)
            if (throwableHookActive) recordException(throwable, context)
        }

        // Fetch interceptor
        fetchInterceptors.add(installedFetchInterceptor)

        // Action instrumentor — creates interaction spans for user-triggered actions
        actionInstrumentors.add(installedActionInstrumentor)

        // Span recorder — enables the public span() function
        spanRecorder = installedSpanRecorder

        // Navigation hook
        bindNavigation(navigator)

        // Lifecycle tracking — cold start span
        val nowNanos = nanosString()
        exporter.addSpan(
            OtlpSpan(
                traceId = currentTraceId,
                spanId = spanId(),
                name = "app.cold_start",
                kind = 1, // SPAN_KIND_INTERNAL
                startTimeUnixNano = coldStartNanos,
                endTimeUnixNano = nowNanos,
                attributes = listOf(
                    OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)),
                    OtlpKeyValue("os.type", OtlpAnyValue(stringValue = Platform.current.name.lowercase())),
                ),
            )
        )

        // Lifecycle tracking — foreground/background sessions
        lastForegroundNanos = nanosString()
        foregroundSpanId = spanId()
        cleanups += AppState.inForeground.addListener {
            if (AppState.inForeground.value) {
                onForeground()
            } else {
                onBackground()
            }
        }

        // Lifecycle tracking — connectivity issues
        cleanups += Connectivity.lastConnectivityIssueCode.addListener {
            val code = Connectivity.lastConnectivityIssueCode.value
            if (code != 0.toShort()) {
                exporter.incrementCounter(
                    "connectivity.issues",
                    attributes = listOf(
                        OtlpKeyValue("connectivity.issue_code", OtlpAnyValue(intValue = code.toLong()))
                    )
                )
                exporter.addLog(
                    OtlpLogRecord(
                        timeUnixNano = nanosString(),
                        severityNumber = OtlpSeverity.WARN.number,
                        severityText = OtlpSeverity.WARN.text,
                        body = OtlpAnyValue(stringValue = "Connectivity issue: status code $code"),
                        attributes = listOf(
                            OtlpKeyValue("connectivity.issue_code", OtlpAnyValue(intValue = code.toLong())),
                            OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)),
                        ),
                    )
                )
            }
        }

        // Crash hook — uses crashHookActive flag so shutdown() can deactivate without
        // needing to un-register platform hooks (which is fragile/impossible on some platforms).
        crashHookActive = true
        installCrashHook { throwable ->
            if (crashHookActive) {
                recordException(throwable, "uncaught", OtlpSeverity.FATAL)
                blockingFlush(exporter)
            }
        }

        // Start the background flush loop
        exporter.startFlushLoop()

        log.info("Telemetry configured: endpoint=${config.endpoint}, service=${config.serviceName}")
    }

    /**
     * Removes all hooks installed by [install] and stops the flush loop.
     * Buffered data is NOT flushed — call [flush] first if you need to drain.
     */
    internal fun shutdown() {
        if (!installed) return
        installed = false
        fetchInterceptors.remove(installedFetchInterceptor)
        Log.interceptors.remove(installedLogInterceptor)
        elementContext?.exceptionHandlers?.remove(exceptionHandlerInterceptor)
        actionInstrumentors.remove(installedActionInstrumentor)
        if (spanRecorder === installedSpanRecorder) spanRecorder = null
        throwableHookActive = false
        crashHookActive = false
        navCleanup?.invoke()
        navCleanup = null
        cleanups.forEach { it() }
        cleanups.clear()
        exporter.stop()
    }

    // ===== Fetch instrumentation =====

    internal suspend fun instrumentFetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
    ): RequestResponse {
        val startMs = clockMillis()
        val startNanos = nanosString()
        val fetchSpanId = spanId()
        val ctx = currentCoroutineContext()
        val fetchTraceId = ctx.traceId().ifEmpty { currentTraceId }
        val parentSpanId = ctx.spanId().ifEmpty { currentSpanId }
        val viewPath = ctx.viewPath()

        // Strip credentials once, from the whole URL, rather than per attribute. The original fix
        // sanitised only `host`, and the `url.path` attribute went on shipping `user:pass@` to the
        // backend regardless. Doing it here means every attribute derived below is safe and a
        // later-added attribute cannot quietly reintroduce the leak. `proceed` still receives the
        // untouched `url`, because the request itself may genuinely need those credentials.
        val telemetryUrl = url.withoutUserInfo()
        val host = telemetryUrl.substringAfter("://").substringBefore("/").substringBefore("?")

        // Copy headers to avoid mutating the caller's HttpHeaders instance
        val outHeaders = httpHeaders(headers)

        // Only inject traceparent if the host is in the propagation allowlist (empty = no propagation)
        if (config.tracePropagationHosts.isNotEmpty() && config.tracePropagationHosts.any { host.endsWith(it) }) {
            val sampled = if (currentTraceIsSampled) "01" else "00"
            outHeaders.set("traceparent", "00-$fetchTraceId-$fetchSpanId-$sampled")
        }

        val response: RequestResponse
        var errorType: String? = null
        try {
            response = proceed(url, method, outHeaders, body)
        } catch (e: Exception) {
            errorType = e::class.simpleName ?: "Unknown"
            val durationMs = clockMillis() - startMs
            val endNanos = nanosString()
            recordFetchTelemetry(fetchTraceId, fetchSpanId, parentSpanId, method, telemetryUrl, host, viewPath, startNanos, endNanos, durationMs, errorType = errorType)
            throw e
        }

        val durationMs = clockMillis() - startMs
        val endNanos = nanosString()
        val statusCode = response.status
        errorType = if (statusCode >= 500) "HTTP $statusCode" else null
        recordFetchTelemetry(fetchTraceId, fetchSpanId, parentSpanId, method, telemetryUrl, host, viewPath, startNanos, endNanos, durationMs, errorType, statusCode)

        return response
    }

    private fun recordFetchTelemetry(
        fetchTraceId: String, fetchSpanId: String, parentSpanId: String,
        method: HttpMethod, url: String, host: String, viewPath: String,
        startNanos: String, endNanos: String, durationMs: Double,
        errorType: String?, statusCode: Short? = null,
    ) {
        if (currentTraceIsSampled) {
            exporter.addSpan(
                OtlpSpan(
                    traceId = fetchTraceId,
                    spanId = fetchSpanId,
                    parentSpanId = parentSpanId,
                    name = "HTTP ${method.name}",
                    kind = 3, // SPAN_KIND_CLIENT
                    startTimeUnixNano = startNanos,
                    endTimeUnixNano = endNanos,
                    attributes = buildList {
                        add(OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = method.name)))
                        // Strip query/fragment to avoid leaking tokens, PII, or API keys in URLs
                        add(OtlpKeyValue("url.path", OtlpAnyValue(stringValue = url.substringBefore("?").substringBefore("#"))))
                        add(OtlpKeyValue("server.address", OtlpAnyValue(stringValue = host)))
                        if (statusCode != null) add(OtlpKeyValue("http.response.status_code", OtlpAnyValue(intValue = statusCode.toLong())))
                        if (viewPath.isNotEmpty()) add(OtlpKeyValue("view.path", OtlpAnyValue(stringValue = viewPath)))
                        if (errorType != null) add(OtlpKeyValue("error.type", OtlpAnyValue(stringValue = errorType)))
                    },
                    status = if (errorType != null) OtlpSpanStatus(code = 2, message = errorType) else null,
                )
            )
        }

        exporter.recordHistogram(
            name = "http.client.request.duration",
            value = durationMs,
            unit = "ms",
            attributes = buildList {
                add(OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = method.name)))
                add(OtlpKeyValue("server.address", OtlpAnyValue(stringValue = host)))
                if (statusCode != null) add(OtlpKeyValue("http.response.status_code", OtlpAnyValue(intValue = statusCode.toLong())))
                if (errorType != null) add(OtlpKeyValue("error.type", OtlpAnyValue(stringValue = errorType)))
            }
        )
    }

    // ===== Lifecycle (foreground/background) =====

    private fun onForeground() {
        lastForegroundNanos = nanosString()
        foregroundSpanId = spanId()
        exporter.incrementCounter("app.foreground_count")
    }

    private fun onBackground() {
        if (lastForegroundNanos.isEmpty()) return

        if (currentTraceIsSampled) {
            exporter.addSpan(
                OtlpSpan(
                    traceId = currentTraceId,
                    spanId = foregroundSpanId,
                    name = "app.foreground_session",
                    kind = 1, // SPAN_KIND_INTERNAL
                    startTimeUnixNano = lastForegroundNanos,
                    endTimeUnixNano = nanosString(),
                    attributes = listOf(
                        OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)),
                    ),
                )
            )
        }

        lastForegroundNanos = ""

        // Flush aggressively on background; the OS may kill the app
        AppScope.launch { flush() }
    }

    // ===== Navigation tracking =====

    internal fun bindNavigation(navigator: PageNavigator) {
        navCleanup?.invoke()
        navCleanup = navigator.stack.addListener {
            val page = navigator.stack.value.lastOrNull()
            val pageName = page?.let { it::class.simpleName } ?: "empty"

            if (pageName != lastPageName) {
                endCurrentPageSpan()

                // Rotate trace ID so each page visit is its own trace.
                // Skip on first navigation to keep cold_start span in the same trace.
                if (lastPageName != null) {
                    currentTraceId = traceId()
                }

                lastPageName = pageName
                lastPageStartNanos = nanosString()
                lastPageSpanId = spanId()
                currentSpanId = lastPageSpanId

                // Page view counter (always, not sampled — cheap aggregation)
                exporter.incrementCounter(
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
        if (!currentTraceIsSampled) return

        exporter.addSpan(
            OtlpSpan(
                traceId = currentTraceId,
                spanId = lastPageSpanId,
                name = "page: $name",
                kind = 1, // SPAN_KIND_INTERNAL
                startTimeUnixNano = lastPageStartNanos,
                endTimeUnixNano = nanosString(),
                attributes = listOf(
                    OtlpKeyValue("page.name", OtlpAnyValue(stringValue = name)),
                    OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)),
                ),
            )
        )
    }

    // ===== Public API =====

    /**
     * Temporarily enable verbose (DEBUG+) log shipping for investigation.
     * Call with `false` to restore the configured [TelemetryConfig.logMinSeverity].
     */
    internal fun setVerboseLogging(enabled: Boolean) {
        logMinSeverity = if (enabled) OtlpSeverity.DEBUG else config.logMinSeverity
    }

    /** Record a custom counter metric. */
    internal fun counter(name: String, value: Long = 1, attributes: List<OtlpKeyValue> = emptyList()) {
        exporter.incrementCounter(name, value, attributes)
    }

    /** Record a custom histogram metric value. */
    internal fun histogram(name: String, value: Double, unit: String = "ms", attributes: List<OtlpKeyValue> = emptyList()) {
        exporter.recordHistogram(name, value, unit, attributes)
    }

    /** Flush all buffered data immediately. Call before app termination or on background. */
    internal suspend fun flush() {
        exporter.flushAll()
    }

    internal fun recordException(
        throwable: Throwable,
        context: String,
        severity: OtlpSeverity = OtlpSeverity.ERROR,
    ) {
        val stackTrace = throwable.stackTraceToString()
        exporter.addLog(
            OtlpLogRecord(
                timeUnixNano = nanosString(),
                severityNumber = severity.number,
                severityText = severity.text,
                body = OtlpAnyValue(stringValue = stackTrace),
                attributes = buildList {
                    add(OtlpKeyValue("exception.type", OtlpAnyValue(stringValue = throwable::class.simpleName ?: "Unknown")))
                    add(OtlpKeyValue("exception.message", OtlpAnyValue(stringValue = throwable.message ?: "")))
                    add(OtlpKeyValue("exception.stacktrace", OtlpAnyValue(stringValue = stackTrace)))
                    add(OtlpKeyValue("crash.fingerprint", OtlpAnyValue(stringValue = CrashFingerprint.generate(throwable))))
                    if (context.isNotEmpty()) {
                        add(OtlpKeyValue("exception.context", OtlpAnyValue(stringValue = context)))
                    }
                    add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)))
                    lastPageName?.let { add(OtlpKeyValue("page.name", OtlpAnyValue(stringValue = it))) }
                    add(OtlpKeyValue("os.type", OtlpAnyValue(stringValue = Platform.current.name.lowercase())))
                    add(OtlpKeyValue("app.version", OtlpAnyValue(stringValue = Build.version)))
                    add(OtlpKeyValue("app.debug", OtlpAnyValue(boolValue = Build.debug)))
                    try { addAll(config.exceptionAttributes()) } catch (_: Exception) {}
                },
                traceId = activeTraceId.ifEmpty { currentTraceId },
                spanId = activeSpanId.ifEmpty { currentSpanId },
            )
        )
    }

    // ===== Interaction spans (Phase 2) =====

    /**
     * Creates an interaction span for an [Action] invocation.
     * Called by the Action framework when a user-triggered action starts.
     *
     * Returns an [InteractionSpan] that:
     * - Provides a [TelemetryContext] to propagate trace/span IDs into the action's coroutine
     * - Records the interaction span and increments `action.invocations` counter on [InteractionSpan.end]
     */
    internal fun startInteraction(scope: CoroutineScope, actionTitle: String): InteractionSpan {
        val interactionSpanId = spanId()
        val startNanos = nanosString()
        val interactionTraceId = currentTraceId
        val parentSpanId = currentSpanId
        val element = scope.coroutineContext[TelemetryContext]?.element
        val viewPath = element?.viewPath() ?: ""

        val ctx = TelemetryContext(
            traceId = interactionTraceId,
            spanId = interactionSpanId,
            element = element,
            sampled = currentTraceIsSampled,
        )

        return InteractionSpan(ctx) {
            // Always count interactions (not sampled — cheap aggregation)
            exporter.incrementCounter(
                "action.invocations",
                attributes = buildList {
                    add(OtlpKeyValue("action.title", OtlpAnyValue(stringValue = actionTitle)))
                    if (viewPath.isNotEmpty()) add(OtlpKeyValue("view.path", OtlpAnyValue(stringValue = viewPath)))
                }
            )

            if (currentTraceIsSampled) {
                exporter.addSpan(
                    OtlpSpan(
                        traceId = interactionTraceId,
                        spanId = interactionSpanId,
                        parentSpanId = parentSpanId,
                        name = "action: $actionTitle",
                        kind = 1, // SPAN_KIND_INTERNAL
                        startTimeUnixNano = startNanos,
                        endTimeUnixNano = nanosString(),
                        attributes = buildList {
                            add(OtlpKeyValue("action.title", OtlpAnyValue(stringValue = actionTitle)))
                            if (viewPath.isNotEmpty()) add(OtlpKeyValue("view.path", OtlpAnyValue(stringValue = viewPath)))
                            add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)))
                        },
                    )
                )
            }
        }
    }

    // ===== ID Generation (companion — pure functions, no instance state) =====

    internal companion object {
        private const val hexChars = "0123456789abcdef"

        /** 32 hex chars (16 bytes) — W3C trace ID */
        public fun traceId(): String = randomHex(32)

        /** 16 hex chars (8 bytes) — W3C span ID */
        public fun spanId(): String = randomHex(16)

        /** Current time as nanoseconds-since-epoch string, suitable for OTLP timestamps. */
        public fun nanosString(): String {
            val millis = Clock.System.now().toEpochMilliseconds()
            return (millis * 1_000_000L).toString()
        }

        private fun randomHex(length: Int): String {
            val bytes = Random.nextBytes(length / 2)
            return buildString(length) {
                for (b in bytes) {
                    append(hexChars[(b.toInt() shr 4) and 0xf])
                    append(hexChars[b.toInt() and 0xf])
                }
            }
        }
    }
}

/**
 * An in-progress interaction span created by [Telemetry.startInteraction].
 * Provides a [TelemetryContext] for coroutine context propagation and records
 * the span + counter when [end] is called.
 */
internal class InteractionSpan(
    internal val telemetryContext: TelemetryContext,
    private val onEnd: () -> Unit,
) {
    /** The coroutine context element to add to the action's coroutine. */
    val coroutineContext: CoroutineContext get() = telemetryContext

    /** Ends the interaction span and records telemetry. Call from `finally`. */
    fun end() = onEnd()
}

/**
 * Returns this URL with any `user:pass@` userinfo removed from its authority.
 *
 * Credentials embedded in a URL are still credentials once they reach a telemetry backend, where
 * they are retained, indexed, and readable by anyone with dashboard access. Only the authority is
 * examined, because a bare `@` is legal in a path or query and must survive untouched.
 */
private fun String.withoutUserInfo(): String {
    val schemeEnd = indexOf("://")
    if (schemeEnd < 0) return this
    val authorityStart = schemeEnd + 3
    var authorityEnd = length
    for (i in authorityStart until length) {
        if (this[i] == '/' || this[i] == '?' || this[i] == '#') {
            authorityEnd = i
            break
        }
    }
    val at = lastIndexOf('@', authorityEnd - 1)
    if (at < authorityStart) return this
    return substring(0, authorityStart) + substring(at + 1)
}
