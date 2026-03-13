package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.AppScope
import kotlin.random.Random
import kotlin.time.Clock
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
class Telemetry(val config: TelemetryConfig) {

    // ===== Instance state =====

    val sessionId: String = spanId()
    var currentTraceId: String = traceId()
        internal set
    var currentSpanId: String = ""
        internal set

    internal val exporter: TelemetryExporter = TelemetryExporter(config)

    /** Effective log severity — mutable to support [setVerboseLogging]. */
    var logMinSeverity: OtlpSeverity = config.logMinSeverity
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

    // Saved hooks for cleanup in shutdown
    private var previousThrowableReport: ((Throwable, String) -> Unit)? = null

    // ===== Installation =====

    /**
     * Installs all global hooks (log interceptor, exception capture, fetch instrumentation,
     * navigation binding, lifecycle tracking). Call after construction.
     *
     * Separated from construction so tests can create instances without triggering
     * AppScope/lifecycle hooks.
     */
    fun install(navigator: PageNavigator) {
        activeTelemetry = this

        // Log interceptor
        TelemetryLog(this).install()

        // Exception capture — chains into the existing Throwable_report global
        previousThrowableReport = Throwable_report
        val previous = Throwable_report
        Throwable_report = { throwable, context ->
            previous(throwable, context)
            recordException(throwable, context)
        }

        // Fetch interceptor
        fetchInterceptor = { url, method, headers, body, proceed ->
            instrumentFetch(url, method, headers, body, proceed)
        }

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
        AppState.inForeground.addListener {
            if (AppState.inForeground.value) {
                onForeground()
            } else {
                onBackground()
            }
        }

        // Lifecycle tracking — connectivity issues
        Connectivity.lastConnectivityIssueCode.addListener {
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

        // Crash hook — capture uncaught exceptions at FATAL severity and flush before death
        installCrashHook { throwable ->
            recordException(throwable, "uncaught", OtlpSeverity.FATAL)
            blockingFlush(exporter)
        }

        // Start the background flush loop
        exporter.startFlushLoop()

        log.info("Telemetry configured: endpoint=${config.endpoint}, service=${config.serviceName}")
    }

    // ===== Fetch instrumentation =====

    private suspend fun instrumentFetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
    ): RequestResponse {
        val startMs = clockMillis()
        val startNanos = nanosString()
        val fetchSpanId = spanId()
        val ctx = kotlin.coroutines.coroutineContext
        val fetchTraceId = ctx.traceId()
        val parentSpanId = ctx.spanId()

        val sampled = if (config.traceSamplingRate >= 1.0) "01" else "00"
        headers.set("traceparent", "00-$fetchTraceId-$fetchSpanId-$sampled")

        val response = proceed(url, method, headers, body)

        val durationMs = clockMillis() - startMs
        val endNanos = nanosString()
        val host = url.substringAfter("://").substringBefore("/").substringBefore("?")

        // Record span (subject to sampling)
        if (Random.nextDouble() <= config.traceSamplingRate) {
            exporter.addSpan(
                OtlpSpan(
                    traceId = fetchTraceId,
                    spanId = fetchSpanId,
                    parentSpanId = parentSpanId,
                    name = "HTTP ${method.name}",
                    kind = 3, // SPAN_KIND_CLIENT
                    startTimeUnixNano = startNanos,
                    endTimeUnixNano = endNanos,
                    attributes = listOf(
                        OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = method.name)),
                        OtlpKeyValue("url.full", OtlpAnyValue(stringValue = url)),
                        OtlpKeyValue("server.address", OtlpAnyValue(stringValue = host)),
                    ),
                )
            )
        }

        // Always record latency histogram (cheap aggregation, not sampled)
        exporter.recordHistogram(
            name = "http.client.request.duration",
            value = durationMs,
            unit = "ms",
            attributes = listOf(
                OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = method.name)),
                OtlpKeyValue("server.address", OtlpAnyValue(stringValue = host)),
            )
        )

        return response
    }

    // ===== Lifecycle (foreground/background) =====

    private fun onForeground() {
        lastForegroundNanos = nanosString()
        foregroundSpanId = spanId()
        exporter.incrementCounter("app.foreground_count")
    }

    private fun onBackground() {
        if (lastForegroundNanos.isEmpty()) return

        if (Random.nextDouble() <= config.traceSamplingRate) {
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
        if (Random.nextDouble() > config.traceSamplingRate) return

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
     * Call with `false` to restore the default (WARN+).
     */
    fun setVerboseLogging(enabled: Boolean) {
        logMinSeverity = if (enabled) OtlpSeverity.DEBUG else OtlpSeverity.WARN
    }

    /** Record a custom counter metric. */
    fun counter(name: String, value: Long = 1, attributes: List<OtlpKeyValue> = emptyList()) {
        exporter.incrementCounter(name, value, attributes)
    }

    /** Record a custom histogram metric value. */
    fun histogram(name: String, value: Double, unit: String = "ms", attributes: List<OtlpKeyValue> = emptyList()) {
        exporter.recordHistogram(name, value, unit, attributes)
    }

    /** Flush all buffered data immediately. Call before app termination or on background. */
    suspend fun flush() {
        exporter.flushAll()
    }

    /** Shut down telemetry, flush remaining data, and remove hooks. */
    suspend fun shutdown() {
        exporter.flushAll()
        exporter.stop()
        if (activeTelemetry === this) activeTelemetry = null
        fetchInterceptor = null
        previousThrowableReport?.let { Throwable_report = it }
    }

    internal fun recordException(
        throwable: Throwable,
        context: String,
        severity: OtlpSeverity = OtlpSeverity.ERROR,
    ) {
        exporter.addLog(
            OtlpLogRecord(
                timeUnixNano = nanosString(),
                severityNumber = severity.number,
                severityText = severity.text,
                body = OtlpAnyValue(stringValue = throwable.stackTraceToString()),
                attributes = buildList {
                    add(OtlpKeyValue("exception.type", OtlpAnyValue(stringValue = throwable::class.simpleName ?: "Unknown")))
                    add(OtlpKeyValue("exception.message", OtlpAnyValue(stringValue = throwable.message ?: "")))
                    add(OtlpKeyValue("exception.stacktrace", OtlpAnyValue(stringValue = throwable.stackTraceToString())))
                    add(OtlpKeyValue("crash.fingerprint", OtlpAnyValue(stringValue = CrashFingerprint.generate(throwable))))
                    if (context.isNotEmpty()) {
                        add(OtlpKeyValue("exception.context", OtlpAnyValue(stringValue = context)))
                    }
                    add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)))
                },
                traceId = currentTraceId,
                spanId = currentSpanId,
            )
        )
    }

    // ===== ID Generation (companion — pure functions, no instance state) =====

    companion object {
        private const val hexChars = "0123456789abcdef"

        /** 32 hex chars (16 bytes) — W3C trace ID */
        fun traceId(): String = randomHex(32)

        /** 16 hex chars (8 bytes) — W3C span ID */
        fun spanId(): String = randomHex(16)

        /** Current time as nanoseconds-since-epoch string, suitable for OTLP timestamps. */
        fun nanosString(): String {
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
