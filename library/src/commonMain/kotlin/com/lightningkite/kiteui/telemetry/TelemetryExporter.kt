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
        val key = "$name|${attributes.hashCode()}"
        counters.getOrPut(key) { CounterAggregator(name, attributes) }.add(value)
    }

    fun recordHistogram(name: String, value: Double, unit: String = "ms", attributes: List<OtlpKeyValue> = emptyList()) {
        val key = "$name|${attributes.hashCode()}"
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

    private suspend fun flushTraces() {
        if (spanBuffer.isEmpty()) return
        val spans = ArrayList(spanBuffer)
        spanBuffer.clear()
        val payload = OtlpExportTraceRequest(
            listOf(OtlpResourceSpans(resource, listOf(OtlpScopeSpans(scope, spans))))
        )
        sendOtlp("/v1/traces", json.encodeToString(OtlpExportTraceRequest.serializer(), payload))
    }

    private suspend fun flushMetrics() {
        val metrics = mutableListOf<OtlpMetric>()
        val now = Telemetry.nanosString()

        for ((_, counter) in counters) {
            counter.snapshot(now)?.let { metrics.add(it) }
        }
        for ((_, histogram) in histograms) {
            histogram.snapshot(now)?.let { metrics.add(it) }
        }

        if (metrics.isEmpty()) return
        val payload = OtlpExportMetricsRequest(
            listOf(OtlpResourceMetrics(resource, listOf(OtlpScopeMetrics(scope, metrics))))
        )
        sendOtlp("/v1/metrics", json.encodeToString(OtlpExportMetricsRequest.serializer(), payload))
    }

    private suspend fun flushLogs() {
        if (logBuffer.isEmpty()) return
        val logs = ArrayList(logBuffer)
        logBuffer.clear()
        val payload = OtlpExportLogsRequest(
            listOf(OtlpResourceLogs(resource, listOf(OtlpScopeLogs(scope, logs))))
        )
        sendOtlp("/v1/logs", json.encodeToString(OtlpExportLogsRequest.serializer(), payload))
    }

    private suspend fun sendOtlp(path: String, body: String) {
        try {
            // Use suppressConnectivityIssues so telemetry export never triggers
            // the app's ConnectivityGate or retry UI
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
                LogRoot.tag("Telemetry").warn("Export rejected for $path: ${response.status} — $responseBody")
            }
        } catch (e: Exception) {
            // Telemetry export failure is silent; never crash the app for observability
            LogRoot.tag("Telemetry").warn("Export failed for $path: ${e.message}")
        }
    }
}
