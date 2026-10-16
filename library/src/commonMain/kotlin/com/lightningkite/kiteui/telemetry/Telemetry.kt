package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Connectivity
import com.lightningkite.kiteui.FetchInterceptor
import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.LogInterceptor
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.RequestBody
import com.lightningkite.kiteui.RequestResponse
import com.lightningkite.kiteui.Throwable_report
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.fetchInterceptors
import com.lightningkite.kiteui.httpHeaders
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.actionInterceptors
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.viewPath
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.services.Namespaced
import com.lightningkite.services.SettingContext
import com.lightningkite.services.telemetry.Counter
import com.lightningkite.services.telemetry.Histogram
import com.lightningkite.services.telemetry.InFlight
import com.lightningkite.services.telemetry.LogLevel
import com.lightningkite.services.telemetry.MetricUnit
import com.lightningkite.services.telemetry.RecordedError
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryAttributeElement
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryBackend
import com.lightningkite.services.telemetry.TelemetryBatch
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.TelemetryTrace
import com.lightningkite.services.telemetry.currentTelemetryAttributes
import com.lightningkite.services.telemetry.telemetryAttributesOf
import com.lightningkite.services.telemetry.telemetryTrace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import com.lightningkite.kiteui.LogLevel as KiteUiLogLevel

/**
 * KiteUI's telemetry, recorded through Service Abstractions' API and sent through your own server, which adds
 * the signed-in user and forwards it to its telemetry backend. No telemetry credentials ship in the app.
 *
 * ```kotlin
 * val telemetry = Telemetry(
 *     serviceName = "my-app",
 *     serviceVersion = Build.version,
 *     upload = telemetryUpload { api.clientTelemetry.ingest(it) }, // from lightning-server-kiteui
 *     tracePropagationHosts = listOf("api.example.com"),
 * )
 * telemetry.install(context, navigator)
 * ```
 *
 * Once installed, requests, actions and screen visits are traced, logs at [minLogLevel] and above and
 * reported exceptions are recorded, and records are kept on the device until uploaded. Clients record no
 * metrics: they're untrusted, and metric cardinality drives observability cost.
 *
 * @param upload sends one batch. Throw to keep it and retry later with backoff; return to be done with it,
 *   whether it was delivered or permanently refused.
 * @param traceSamplingRate the fraction of sessions whose spans are recorded.
 * @param tracePropagationHosts hosts (and their subdomains) that receive a `traceparent` header, linking their spans to ours.
 */
public class Telemetry(
    serviceName: String,
    serviceVersion: String,
    upload: suspend (TelemetryBatch) -> Unit,
    traceSamplingRate: Double = 1.0,
    private val tracePropagationHosts: List<String> = listOf(),
    private val minLogLevel: LogLevel = LogLevel.Warn,
) : TelemetryBackend {
    internal val sessionId: String = spanId()
    internal val sampled: Boolean = Random.nextDouble() < traceSamplingRate
    internal val outbox = TelemetryOutbox(
        resource = TelemetryAttributes {
            put(TelemetryKeys.Service.name, serviceName)
            put(TelemetryKeys.Service.version, serviceVersion)
            put(TelemetryKeys.Os.type, Platform.current.name.lowercase())
            put(TelemetryKeys.Session.id, sessionId)
            put(TelemetryKeys.Telemetry.sdkName, "kiteui")
            put(appDebug, Build.debug)
        },
        send = upload,
    )
    private val constructedAt = Clock.System.now()

    /** The main navigator's current screen, for records made outside any coroutine. */
    internal var currentScreen: TelemetryAttributes = TelemetryAttributes.empty
    private var mainNavigator: PageNavigator? = null

    // Our own owners record to this instance even before install, so they work in tests without global hooks.
    private val ownContext: SettingContext = object : SettingContext by ClientContext {
        override val telemetryBackend: TelemetryBackend get() = this@Telemetry
    }
    private fun owner(name: String): Namespaced = object : Namespaced {
        override val name: String = name
        override val context: SettingContext = ownContext
    }
    private val http = owner("http")
    private val action = owner("action")
    private val screen = owner("screen")

    // ===== Installation =====

    private var installed = false
    // Flags rather than un-registering: other code may have wrapped these hooks after us.
    private var throwableHookActive = false
    private var crashHookActive = false
    private var elementContext: ElementContext? = null
    private var flushLoop: Job? = null
    private val cleanups = ArrayList<() -> Unit>()

    private val fetchInterceptor: FetchInterceptor = { url, method, headers, body, proceed ->
        instrumentFetch(url, method, headers, body, proceed)
    }
    internal val actionInterceptor: suspend (CoroutineScope, String, suspend () -> Unit) -> Unit = { scope, title, proceed ->
        // Actions run in AppScope by default, which doesn't carry the screen's attributes.
        withContext(scope.coroutineContext[TelemetryAttributeElement] ?: EmptyCoroutineContext) {
            action.telemetryTrace(title) { proceed() }
        }
    }
    internal val logInterceptor: LogInterceptor = LogInterceptor { level, tag, entries ->
        val telemetryLevel = when (level) {
            KiteUiLogLevel.ERROR -> LogLevel.Error
            KiteUiLogLevel.WARN -> LogLevel.Warn
            KiteUiLogLevel.INFO -> LogLevel.Info
            KiteUiLogLevel.LOG -> LogLevel.Debug
        }
        if (telemetryLevel >= minLogLevel) {
            val attributes = if (tag.isEmpty()) currentScreen else currentScreen + telemetryAttributesOf(logTag to tag)
            enqueue { add(RecordedLog(Clock.System.now(), telemetryLevel, entries.joinToString(" "), attributes)) }
        }
    }
    private val exceptionHandlerInterceptor = ExceptionHandler(10f) { e, meta ->
        val source = meta?.source
        reportError(e, TelemetryAttributes {
            if (source != null) {
                source.underlyingNativeElement.coroutineContext[TelemetryAttributeElement]?.let { putAll(it.flattened()) }
                put(viewPathKey, source.viewPath())
            }
        })
        null // observe only; let the app's handlers show it
    }

    /** Installs the global hooks and starts uploading. [navigator] is the one whose screen labels logs. */
    public fun install(context: ElementContext, navigator: PageNavigator) {
        check(!installed) { "Telemetry.install() called twice. Call shutdown() first." }
        installed = true
        mainNavigator = navigator
        ClientContext.telemetryBackend = this

        Log.interceptors.add(logInterceptor)
        elementContext = context
        context.exceptionHandlers += exceptionHandlerInterceptor
        throwableHookActive = true
        val previous = Throwable_report
        Throwable_report = { throwable, where ->
            previous(throwable, where)
            if (throwableHookActive) reportError(throwable, telemetryAttributesOf(exceptionContext to where))
        }
        fetchInterceptors.add(fetchInterceptor)
        actionInterceptors.add(actionInterceptor)

        if (sampled) enqueue {
            add(RecordedSpan(traceId(), spanId(), null, "app", "start", constructedAt, Clock.System.now(),
                TelemetryAttributes.empty, RecordedSpan.Outcome.Ok, null))
        }

        cleanups += AppState.inForeground.addListener {
            if (AppState.inForeground.value) {
                event("app.foreground", currentScreen)
            } else {
                event("app.background", currentScreen)
                // The OS may kill a backgrounded app without warning.
                outbox.persist()
                AppScope.launch { outbox.upload() }
            }
        }

        cleanups += Connectivity.lastConnectivityIssueCode.addListener {
            val code = Connectivity.lastConnectivityIssueCode.value
            if (code != 0.toShort()) {
                val attributes = telemetryAttributesOf(connectivityIssueCode to code.toLong())
                enqueue {
                    add(RecordedLog(Clock.System.now(), LogLevel.Warn, "Connectivity issue: status code $code", currentScreen + attributes))
                }
            }
        }

        crashHookActive = true
        installCrashHook { throwable ->
            if (crashHookActive) {
                outbox.add(RecordedLog(Clock.System.now(), LogLevel.Error, throwable.message ?: throwable::class.simpleName ?: "Crash",
                    currentScreen + telemetryAttributesOf(TelemetryKeys.Exception.escaped to true), error = throwable.toRecordedError()))
                outbox.persist(durably = true)
            }
        }

        flushLoop = AppScope.launch {
            outbox.upload() // whatever earlier runs left behind
            while (true) {
                delay(30.seconds)
                outbox.persist()
                outbox.upload()
            }
        }
    }

    /** Removes the hooks [install] added. Records not yet persisted are lost; call [flush] first to keep them. */
    internal fun shutdown() {
        if (!installed) return
        installed = false
        ClientContext.telemetryBackend = TelemetryBackend.Noop
        fetchInterceptors.remove(fetchInterceptor)
        actionInterceptors.remove(actionInterceptor)
        Log.interceptors.remove(logInterceptor)
        elementContext?.exceptionHandlers?.remove(exceptionHandlerInterceptor)
        throwableHookActive = false
        crashHookActive = false
        cleanups.forEach { it() }
        cleanups.clear()
        flushLoop?.cancel()
    }

    // ===== Recording =====

    private fun enqueue(add: TelemetryOutbox.() -> Unit) {
        outbox.add()
        if (outbox.bufferedRecords >= MAX_BUFFERED_RECORDS) {
            outbox.persist()
            if (installed) AppScope.launch { outbox.upload() }
        }
    }

    private fun event(name: String, attributes: TelemetryAttributes) = enqueue {
        add(RecordedLog(Clock.System.now(), LogLevel.Info, name, attributes + telemetryAttributesOf(TelemetryKeys.Event.name to name)))
    }

    override suspend fun <T> span(
        owner: Namespaced,
        opName: String,
        attributes: TelemetryAttributes,
        dimensions: Set<TelemetryKey<*>>,
        action: suspend (TelemetryTrace) -> T,
    ): T {
        val parent = currentCoroutineContext()[ClientSpan]
        val span = ClientSpan(traceId = parent?.traceId ?: traceId(), spanId = spanId())
        val trace = ClientTrace(span)
        val start = Clock.System.now()
        var outcome = RecordedSpan.Outcome.Error
        var error: RecordedError? = null
        try {
            return withContext(span) { action(trace) }.also { outcome = RecordedSpan.Outcome.Ok }
        } catch (e: CancellationException) {
            outcome = RecordedSpan.Outcome.Cancelled
            throw e
        } catch (e: Throwable) {
            error = e.toRecordedError()
            throw e
        } finally {
            if (sampled) enqueue {
                add(RecordedSpan(span.traceId, span.spanId, parent?.spanId, owner.name, opName, start, Clock.System.now(),
                    attributes + trace.enriched, outcome, error))
            }
        }
    }

    private inner class ClientTrace(private val span: ClientSpan) : TelemetryTrace {
        var enriched = TelemetryAttributes.empty
        override fun enrich(attributes: TelemetryAttributes) { enriched += attributes }
        override fun isLoggable(level: LogLevel): Boolean = level >= minLogLevel
        override fun log(level: LogLevel, message: String, attributes: TelemetryAttributes) {
            if (isLoggable(level)) enqueue { add(RecordedLog(Clock.System.now(), level, message, attributes, span.traceId, span.spanId)) }
        }
    }

    override fun counter(owner: Namespaced, name: String, unit: MetricUnit, dimensions: Set<TelemetryKey<*>>): Counter =
        throw UnsupportedOperationException("Clients don't record metrics; record a span or log instead.")

    override fun histogram(owner: Namespaced, name: String, unit: MetricUnit, dimensions: Set<TelemetryKey<*>>): Histogram =
        throw UnsupportedOperationException("Clients don't record metrics; record a span or log instead.")

    override fun inFlight(owner: Namespaced, name: String, dimensions: Set<TelemetryKey<*>>): InFlight =
        throw UnsupportedOperationException("In-flight gauges aren't supported on clients.")

    override fun gauge(owner: Namespaced, name: String, unit: MetricUnit, attributes: TelemetryAttributes, sample: () -> Long): AutoCloseable =
        throw UnsupportedOperationException("Gauges aren't supported on clients.")

    override fun reportError(throwable: Throwable, attributes: TelemetryAttributes): Unit = enqueue {
        add(RecordedLog(Clock.System.now(), LogLevel.Error, throwable.message ?: throwable::class.simpleName ?: "Error",
            currentScreen + attributes, error = throwable.toRecordedError()))
    }

    override suspend fun flush() {
        outbox.persist()
        outbox.upload()
    }

    override fun ingest(batch: TelemetryBatch): Unit =
        throw UnsupportedOperationException("Clients record telemetry; servers ingest it.")

    // ===== Instrumentation =====

    internal suspend fun instrumentFetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
    ): RequestResponse {
        if (currentCoroutineContext()[TelemetryUploading] != null) return proceed(url, method, headers, body)

        // Strip credentials once, from the whole URL, so no attribute derived below can carry them.
        // `proceed` still receives the untouched `url`, because the request itself may need them.
        val afterScheme = url.withoutUserInfo().substringAfter("://").substringBefore("?").substringBefore("#") // queries carry tokens
        val host = afterScheme.substringBefore("/").substringBefore(":")
        val attributes = TelemetryAttributes {
            put(TelemetryKeys.Http.requestMethod, method.name)
            put(TelemetryKeys.Url.path, "/" + afterScheme.substringAfter("/", ""))
            put(TelemetryKeys.Server.address, host)
        }
        return http.telemetryTrace(method.name, attributes) { trace ->
            val outHeaders = httpHeaders(headers)
            if (tracePropagationHosts.any { host == it || host.endsWith(".$it") }) {
                val span = currentCoroutineContext()[ClientSpan]!!
                outHeaders.set("traceparent", "00-${span.traceId}-${span.spanId}-${if (sampled) "01" else "00"}")
            }
            val response = proceed(url, method, outHeaders, body)
            trace.enrich(telemetryAttributesOf(TelemetryKeys.Http.responseStatusCode to response.status.toLong()))
            response
        }
    }

    /** Runs for as long as a screen is shown, with the screen's attributes in the coroutine context. */
    internal suspend fun recordScreenVisit(navigator: PageNavigator, load: ScreenLoad) {
        val screenAttributes = currentTelemetryAttributes()
        if (navigator === mainNavigator) currentScreen = screenAttributes
        event("app.screen.view_start", screenAttributes)
        try {
            try {
                screen.telemetryTrace("load") {
                    withTimeoutOrNull(60.seconds) { load.usable.await() } ?: throw ScreenLoadTimeout()
                }
            } catch (_: ScreenLoadTimeout) {
                // Recorded as the load span's error; the visit goes on.
            }
            awaitCancellation()
        } finally {
            event("app.screen.view_end", screenAttributes)
        }
    }

    internal companion object {
        const val MAX_BUFFERED_RECORDS = 100

        private val appDebug = TelemetryKey.OfBoolean("app.debug")
        private val logTag = TelemetryKey.OfString("log.tag")
        private val exceptionContext = TelemetryKey.OfString("exception.context")
        private val viewPathKey = TelemetryKey.OfString("view.path")
        private val connectivityIssueCode = TelemetryKey.OfLong("connectivity.issue_code")

        /** 32 hex chars (16 bytes), a W3C trace id. */
        fun traceId(): String = randomHex(16)

        /** 16 hex chars (8 bytes), a W3C span id. */
        fun spanId(): String = randomHex(8)

        @OptIn(ExperimentalStdlibApi::class)
        private fun randomHex(bytes: Int): String = Random.nextBytes(bytes).toHexString()
    }
}

private class ScreenLoadTimeout : Exception("The screen wasn't usable within 60 seconds")

/** The client span the current coroutine runs in; spans started inside it become its children. */
internal class ClientSpan(val traceId: String, val spanId: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ClientSpan>
}

private fun Throwable.toRecordedError(): RecordedError = RecordedError(
    type = this::class.simpleName ?: "Unknown",
    message = message ?: "",
    stacktrace = stackTraceToString(),
    fingerprint = CrashFingerprint.generate(this),
)

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
