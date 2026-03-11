package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.lightningkite.kiteui.telemetry.*
import kotlin.time.Clock
import kotlin.time.Instant


class WaitGate(permit: Boolean = false) {
    var permit: Boolean = permit
        set(value) {
            field = value
            if (value) {
                for (continuation in continuations) {
                    continuation.resume(Unit)
                }
                continuations.clear()
            }
        }
    fun permitOnce() {
        permit = true
        permit = false
    }
    val continuations = ArrayList<Continuation<Unit>>()
    suspend fun await(): Unit {
        if (permit) return
        else return suspendCancellableCoroutine {
            continuations.add(it)
            it.invokeOnCancellation { _ -> continuations.remove(it) }
        }
    }
    fun abandon() {
        for (continuation in continuations) {
            continuation.resumeWithException(CancellationException("abandoned as requested"))
        }
        continuations.clear()
    }
}

class ConnectivityGate(val clock: Clock = Clock.System, val delay: suspend (ms: Long) -> Unit = { ms -> kotlinx.coroutines.delay(ms) }) {
    val gate = WaitGate(true)
    val baseRetry = 10.seconds
    var nextRetry = baseRetry
    val maxRetry = 5.minutes
    val retryAt = Signal<Instant?>(null)

    fun retryNow() {
        retryAt.value = null
        gate.permit = true
    }

    fun abandon() {
        gate.abandon()
        retryAt.value = null
        gate.permit = true
    }

    suspend fun <T> run(tag: String, action: suspend () -> T): T {
        while (true) {
            gate.await()
            try {
                val r = action()
                nextRetry = baseRetry
                return r
            } catch (e: ConnectionException) {
                e.printStackTrace2()
                if (retryAt.value == null) {
                    AppScope.launch {
                        val d = nextRetry
                        retryAt.value = clock.now() + d
                        nextRetry = d.times(2).coerceAtMost(maxRetry)
                        gate.permit = false
                        delay(d.inWholeMilliseconds)
                        retryNow()
                    }
                }
            }
        }
    }
}

@Deprecated("Use Connectivity instead", ReplaceWith("Connectivity.fetchGate", "com.lightningkite.kiteui.Connectivity"))
val connectivityFetchGate get() = Connectivity.fetchGate

object Connectivity {
    val noConnectivityCodes = setOf<Short>(502, 503)
    val tooMuchCodes = setOf<Short>(420, 429)
    val stopConnectivityCodes = noConnectivityCodes + tooMuchCodes
    val fetchGate = ConnectivityGate()
    val lastConnectivityIssueCode: Signal<Short> = Signal(0)
}

suspend fun connectivityFetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: suspend () -> HttpHeaders = { httpHeaders() },
    body: RequestBody?,
): RequestResponse {
    // by Claude - telemetry: capture timing, trace context, and span ID BEFORE any suspension.
    // Read from coroutineContext so structured concurrency carries the right page span,
    // even if the user navigates while this request is in flight.
    val telemetryActive = Telemetry.isActive
    val startMs = if (telemetryActive) clockMillis() else 0.0
    val startNanos = if (telemetryActive) IdGenerator.nanosString() else ""
    val spanId = if (telemetryActive) IdGenerator.spanId() else ""
    val traceId = if (telemetryActive) coroutineContext.traceId() else ""
    val parentSpanId = if (telemetryActive) coroutineContext.spanId() else ""

    // by Claude - wrap headers to inject W3C traceparent, giving the server end-to-end correlation
    val tracedHeaders: suspend () -> HttpHeaders = if (telemetryActive) {
        {
            headers().also { h ->
                val sampled = if ((Telemetry.config?.traceSamplingRate ?: 1.0) >= 1.0) "01" else "00"
                h.set("traceparent", "00-$traceId-$spanId-$sampled")
            }
        }
    } else headers

    val response = if(coroutineContext[ConnectivityIssueSuppress.Key] == null) {
        Connectivity.fetchGate.run("$method $url") {
            val response = try {
                fetch(url = url, method = method, headers = tracedHeaders(), body = body)
            } catch(e: ConnectionException) {
                // Perform a single retry immediately
                Log.warn("Forced retry on $method $url")
                try {
                    fetch(url = url, method = method, headers = tracedHeaders(), body = body)
                } catch(e: ConnectionException) {
                    Connectivity.lastConnectivityIssueCode.value = 0
                    throw e
                }
            }
            if (response.status in Connectivity.stopConnectivityCodes) {
                Connectivity.lastConnectivityIssueCode.value = response.status
                throw ConnectionException("Status code ${response.status}")
            }
            response
        }
    } else {
        fetch(url = url, method = method, headers = tracedHeaders(), body = body)
    }

    // by Claude - telemetry: record HTTP span and latency histogram
    if (telemetryActive) {
        val durationMs = clockMillis() - startMs
        val endNanos = IdGenerator.nanosString()
        val host = url.substringAfter("://").substringBefore("/").substringBefore("?")
        val exporter = Telemetry.exporter

        // Record span (subject to sampling)
        if (exporter != null && kotlin.random.Random.nextDouble() <= (Telemetry.config?.traceSamplingRate ?: 1.0)) {
            exporter.addSpan(
                OtlpSpan(
                    traceId = traceId,
                    spanId = spanId,
                    parentSpanId = parentSpanId, // links to the page span, captured before suspension
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
        exporter?.recordHistogram(
            name = "http.client.request.duration",
            value = durationMs,
            unit = "ms",
            attributes = listOf(
                OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = method.name)),
                OtlpKeyValue("server.address", OtlpAnyValue(stringValue = host)),
            )
        )
    }

    return response
}

class ConnectivityIssueSuppress(): CoroutineContext.Element {
    override val key: CoroutineContext.Key<ConnectivityIssueSuppress> = Key
    object Key: CoroutineContext.Key<ConnectivityIssueSuppress>
}
suspend fun <T> suppressConnectivityIssues(action: suspend () -> T): T {
    return withContext(ConnectivityIssueSuppress()) { action() }
}