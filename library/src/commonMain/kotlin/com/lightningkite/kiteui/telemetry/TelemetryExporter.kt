package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.*
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class TelemetryExporter(private val config: TelemetryConfig) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    // Buffers are internal for test access; accessed only from main thread (same threading model as all KiteUI)
    internal val spanBuffer = ArrayDeque<OtlpSpan>()
    internal val logBuffer = ArrayDeque<OtlpLogRecord>()

    // Metric aggregators keyed by "name|attributesHash"
    internal val counters = HashMap<String, CounterAggregator>()
    internal val histograms = HashMap<String, HistogramAggregator>()

    private var flushJob: Job? = null

    private val resource: OtlpResource by lazy {
        OtlpResource(
            attributes = listOf(
                OtlpKeyValue("service.name", OtlpAnyValue(stringValue = config.serviceName)),
                OtlpKeyValue("service.version", OtlpAnyValue(stringValue = config.serviceVersion)),
                OtlpKeyValue("telemetry.sdk.name", OtlpAnyValue(stringValue = "kiteui")),
                OtlpKeyValue("telemetry.sdk.language", OtlpAnyValue(stringValue = "kotlin")),
                OtlpKeyValue("os.type", OtlpAnyValue(stringValue = Platform.current.name.lowercase())),
            )
        )
    }

    private val scope = OtlpInstrumentationScope(
        name = "com.lightningkite.kiteui",
        version = config.serviceVersion
    )

    fun addSpan(span: OtlpSpan) {
        if (spanBuffer.size >= config.maxQueueSize) spanBuffer.removeFirst()
        spanBuffer.addLast(span)
        if (spanBuffer.size >= config.maxBatchSize) {
            AppScope.launch { flushTraces() }
        }
    }

    fun addLog(logRecord: OtlpLogRecord) {
        if (logBuffer.size >= config.maxQueueSize) logBuffer.removeFirst()
        logBuffer.addLast(logRecord)
        if (logBuffer.size >= config.maxBatchSize) {
            AppScope.launch { flushLogs() }
        }
    }

    fun incrementCounter(name: String, value: Long = 1, attributes: List<OtlpKeyValue> = emptyList()) {
        val key = "$name|${attributes.joinToString(",") { "${it.key}=${it.value}" }}"
        counters.getOrPut(key) { CounterAggregator(name, attributes) }.add(value)
    }

    fun recordHistogram(name: String, value: Double, unit: String = "ms", attributes: List<OtlpKeyValue> = emptyList()) {
        val key = "$name|${attributes.joinToString(",") { "${it.key}=${it.value}" }}"
        histograms.getOrPut(key) { HistogramAggregator(name, unit, attributes) }.record(value)
    }

    fun startFlushLoop() {
        flushJob = AppScope.launch {
            while (true) {
                kotlinx.coroutines.delay(config.flushIntervalMs)
                flushAll()
            }
        }
    }

    fun stop() {
        flushJob?.cancel()
        flushJob = null
    }

    suspend fun flushAll() {
        flushTraces()
        flushMetrics()
        flushLogs()
    }

    private fun drainTraces(): Pair<String, String>? {
        if (spanBuffer.isEmpty()) return null
        val spans = ArrayList(spanBuffer)
        spanBuffer.clear()
        val payload = OtlpExportTraceRequest(
            listOf(OtlpResourceSpans(resource, listOf(OtlpScopeSpans(scope, spans))))
        )
        return "/v1/traces" to json.encodeToString(OtlpExportTraceRequest.serializer(), payload)
    }

    private fun drainMetrics(): Pair<String, String>? {
        val metrics = mutableListOf<OtlpMetric>()
        val now = Telemetry.nanosString()
        for ((_, counter) in counters) { counter.snapshot(now)?.let { metrics.add(it) } }
        for ((_, histogram) in histograms) { histogram.snapshot(now)?.let { metrics.add(it) } }
        if (metrics.isEmpty()) return null
        val payload = OtlpExportMetricsRequest(
            listOf(OtlpResourceMetrics(resource, listOf(OtlpScopeMetrics(scope, metrics))))
        )
        return "/v1/metrics" to json.encodeToString(OtlpExportMetricsRequest.serializer(), payload)
    }

    private fun drainLogs(): Pair<String, String>? {
        if (logBuffer.isEmpty()) return null
        val logs = ArrayList(logBuffer)
        logBuffer.clear()
        val payload = OtlpExportLogsRequest(
            listOf(OtlpResourceLogs(resource, listOf(OtlpScopeLogs(scope, logs))))
        )
        return "/v1/logs" to json.encodeToString(OtlpExportLogsRequest.serializer(), payload)
    }

    private suspend fun flushTraces() { drainTraces()?.let { sendOtlp(it.first, it.second) } }
    private suspend fun flushMetrics() { drainMetrics()?.let { sendOtlp(it.first, it.second) } }
    private suspend fun flushLogs() { drainLogs()?.let { sendOtlp(it.first, it.second) } }

    /**
     * Serializes all buffered data into (fullUrl, jsonBody) pairs without sending.
     * Used by platforms (JS) that need synchronous fire-and-forget export (e.g. sendBeacon).
     */
    internal fun drainToPayloads(): List<Pair<String, String>> {
        val baseUrl = config.endpoint.trimEnd('/')
        return listOfNotNull(drainTraces(), drainMetrics(), drainLogs())
            .map { (path, body) -> "$baseUrl$path" to body }
    }

    private suspend fun sendOtlp(path: String, body: String) {
        try {
            // Use suppressConnectivityIssues so telemetry export never triggers
            // the app's ConnectivityGate or retry UI
            // fetchRaw (not fetch) to bypass fetchInterceptors — using fetch here would
            // cause infinite recursion: export → fetch → telemetry interceptor → export → ...
            val response = suppressConnectivityIssues {
                fetchRaw(
                    url = config.endpoint.trimEnd('/') + path,
                    method = HttpMethod.POST,
                    headers = httpHeaders(
                        config.headers.toList() + ("Content-Type" to "application/json")
                    ),
                    body = RequestBodyText(body, "application/json")
                )
            }
            if (!response.ok) {
                val responseBody = try { response.text() } catch (_: Exception) { "(unreadable)" }
                // Use LogRoot directly (not Log) to bypass the telemetry log interceptor.
                // Using Log here would cause infinite recursion: export fail → log → export → fail → ...
                LogRoot.tag("Telemetry").warn("Export rejected for $path: ${response.status} — $responseBody")
            }
        } catch (e: Exception) {
            // Telemetry export failure is silent; never crash the app for observability.
            // LogRoot bypasses the telemetry log interceptor to prevent infinite recursion.
            LogRoot.tag("Telemetry").warn("Export failed for $path: ${e.message}")
        }
    }
}
