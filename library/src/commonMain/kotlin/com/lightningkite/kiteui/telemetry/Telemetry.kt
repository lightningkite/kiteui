package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Connectivity
import com.lightningkite.kiteui.ThrowableReporter
import com.lightningkite.kiteui.HttpFetcher
import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.RequestBody
import com.lightningkite.kiteui.RequestResponse
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.ssr.ssrResourceRegistry
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.viewPath
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.services.telemetry.LogLevel
import com.lightningkite.services.telemetry.RecordedError
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryBatch
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.CancellationException
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
 * KiteUI's telemetry, sent through your own server, which adds the signed-in user and forwards it to its
 * telemetry backend. No telemetry credentials ship in the app.
 *
 * ```kotlin
 * val telemetry = Telemetry(
 *     serviceVersion = Build.version,
 *     upload = telemetryUpload { api.clientTelemetry.ingest(it) }, // from lightning-server-kiteui
 *     tracePropagationHosts = listOf("api.example.com"),
 * )
 * telemetry.install(context, navigator)
 * ```
 *
 * Once installed, requests, actions and screen visits are traced, logs at [minLogLevel] and above and
 * reported exceptions are recorded, and records are kept on the device until uploaded. Record your own spans
 * with [span]. No metrics are recorded.
 *
 * @param upload sends one batch. Throw to keep it and retry later with backoff; return to be done with it,
 *   whether it was delivered or permanently refused.
 * @param traceSamplingRate the fraction of sessions whose spans are recorded.
 * @param tracePropagationHosts hosts (and their subdomains) that receive a `traceparent` header, linking their spans to ours.
 *   Requests to relative URLs, which go to the web page's own origin, receive one whenever this isn't empty.
 */
public class Telemetry(
    serviceVersion: String,
    upload: suspend (TelemetryBatch) -> Unit,
    traceSamplingRate: Double = 1.0,
    private val tracePropagationHosts: List<String> = listOf(),
    private val minLogLevel: LogLevel = LogLevel.Warn,
) {
    internal val sessionId: String = spanId()
    internal val sampled: Boolean = Random.nextDouble() < traceSamplingRate
    internal val outbox = TelemetryOutbox(
        // The server keeps only these keys (LS ClientTelemetryEndpoints.CLIENT_RESOURCE_KEYS) and sets service.name itself.
        resource = TelemetryAttributes {
            put(TelemetryKeys.Service.version, serviceVersion)
            put(TelemetryKeys.Os.type, Platform.current.name.lowercase())
            put(TelemetryKeys.Session.id, sessionId)
            put(appDebug, Build.debug)
        },
        send = upload,
    )
    private val constructedAt = Clock.System.now()

    /** The main navigator's current screen, for records made outside any coroutine. */
    internal var currentScreen: TelemetryAttributes = TelemetryAttributes.empty
    private var mainNavigator: PageNavigator? = null

    /**
     * Installs the global hooks and starts uploading, for the rest of the process. [navigator] is the one whose
     * screen labels logs.
     *
     * Only one [Telemetry] can be installed. During server-side rendering this does nothing, since the app runs once
     * per request inside the server's process, which isn't a client.
     */
    public fun install(context: ElementContext, navigator: PageNavigator) {
        if (context.ssrResourceRegistry != null) return
        check(Companion.installed == null) { "A Telemetry is already installed." }
        Companion.installed = this
        mainNavigator = navigator

        HttpFetcher.Interceptor.global.register(HttpFetcher.Interceptor(::instrumentFetch))
        Action.Interceptor.global.register(actionInterceptor)
        Log.Interceptor.global.register(logInterceptor)
        context.exceptionHandlers += ExceptionHandler(Float.MAX_VALUE) { e, meta ->
            reportElementError(e, meta?.source)
            null // observe only; let the app's handlers show it
        }
        ThrowableReporter.global.register { throwable, where ->
            reportError(throwable, telemetryAttributesOf(exceptionContext to where))
        }
        installCrashHook(::recordCrash)
        AppState.inForeground.addListener(::onForegroundChanged)
        Connectivity.lastConnectivityIssueCode.addListener(::onConnectivityIssue)

        if (sampled) enqueue {
            add(RecordedSpan(traceId(), spanId(), null, "app", "start", constructedAt, Clock.System.now(),
                TelemetryAttributes.empty, RecordedSpan.Outcome.Ok, null))
        }
        AppScope.launch {
            outbox.upload() // whatever earlier runs left behind
            while (true) {
                delay(30.seconds)
                outbox.persist()
                outbox.upload()
            }
        }
    }

    internal val actionInterceptor = Action.Interceptor { scope, title, proceed ->
        // Actions run in AppScope by default, which carries neither the screen's attributes nor, for the halves of a
        // combined action, the combined action's span.
        val started = scope.coroutineContext
        withContext((started[TelemetryAttributesElement] ?: EmptyCoroutineContext) + (started[ClientSpan] ?: EmptyCoroutineContext)) {
            span("action", title) { proceed() }
        }
    }

    internal val logInterceptor = Log.Interceptor { level, tag, entries ->
        val telemetryLevel = when (level) {
            KiteUiLogLevel.ERROR -> LogLevel.Error
            KiteUiLogLevel.WARN -> LogLevel.Warn
            KiteUiLogLevel.INFO -> LogLevel.Info
            KiteUiLogLevel.LOG -> LogLevel.Debug
        }
        if (telemetryLevel >= minLogLevel) {
            val attributes = if (tag.isEmpty()) currentScreen else currentScreen + telemetryAttributesOf(logTag to tag)
            log(telemetryLevel, entries.joinToString(" "), attributes)
        }
    }

    private fun reportElementError(throwable: Throwable, source: Element?) = reportError(throwable, TelemetryAttributes {
        if (source != null) {
            source.underlyingNativeElement.coroutineContext[TelemetryAttributesElement]?.let { putAll(it.attributes) }
            put(viewPathKey, source.viewPath())
        }
    })

    private fun onForegroundChanged() {
        if (AppState.inForeground.value) {
            event("app.foreground", currentScreen)
        } else {
            event("app.background", currentScreen)
            // The OS may kill a backgrounded app without warning.
            outbox.persist()
            AppScope.launch { outbox.upload() }
        }
    }

    private fun onConnectivityIssue() {
        val code = Connectivity.lastConnectivityIssueCode.value
        if (code != 0.toShort()) {
            log(LogLevel.Warn, "Connectivity issue: status code $code", currentScreen + telemetryAttributesOf(connectivityIssueCode to code.toLong()))
        }
    }

    // Claude: straight to the outbox rather than through enqueue, which may launch an upload the dying process can't finish.
    private fun recordCrash(throwable: Throwable) {
        outbox.add(RecordedLog(Clock.System.now(), LogLevel.Error, throwable.message ?: throwable::class.simpleName ?: "Crash",
            currentScreen + telemetryAttributesOf(TelemetryKeys.Exception.escaped to true), error = throwable.toRecordedError()))
        outbox.persist(durably = true)
    }

    private fun enqueue(add: TelemetryOutbox.() -> Unit) {
        outbox.add()
        if (outbox.bufferedRecords >= MAX_BUFFERED_RECORDS) {
            outbox.persist()
            if (Companion.installed === this) AppScope.launch { outbox.upload() }
        }
    }

    private fun log(level: LogLevel, message: String, attributes: TelemetryAttributes, error: RecordedError? = null) = enqueue {
        add(RecordedLog(Clock.System.now(), level, message, attributes, error = error))
    }

    private fun event(name: String, attributes: TelemetryAttributes) =
        log(LogLevel.Info, name, attributes + telemetryAttributesOf(TelemetryKeys.Event.name to name))

    internal fun reportError(throwable: Throwable, attributes: TelemetryAttributes): Unit =
        log(LogLevel.Error, throwable.message ?: throwable::class.simpleName ?: "Error", currentScreen + attributes, throwable.toRecordedError())

    /** Runs [action] as span `owner.operation`, a child of the current span if there is one. */
    internal suspend fun <T> span(owner: String, operation: String, attributes: TelemetryAttributes = TelemetryAttributes.empty, action: suspend (ClientSpan) -> T): T {
        val context = currentCoroutineContext()
        val parent = context[ClientSpan]
        val ambient = context[TelemetryAttributesElement]?.attributes ?: TelemetryAttributes.empty
        val span = ClientSpan(traceId = parent?.traceId ?: traceId(), spanId = spanId(), attributes = ambient + attributes)
        val start = Clock.System.now()
        var outcome = RecordedSpan.Outcome.Error
        var error: RecordedError? = null
        try {
            return withContext(span) { action(span) }.also { outcome = if (span.failed) RecordedSpan.Outcome.Error else RecordedSpan.Outcome.Ok }
        } catch (e: CancellationException) {
            outcome = RecordedSpan.Outcome.Cancelled
            throw e
        } catch (e: Throwable) {
            if (sampled) error = e.toRecordedError() // a stack trace and a fingerprint aren't free
            throw e
        } finally {
            if (sampled) enqueue {
                add(RecordedSpan(span.traceId, span.spanId, parent?.spanId, owner, operation, start, Clock.System.now(),
                    span.attributes, outcome, error))
            }
        }
    }

    /**
     * Saves what's been recorded and uploads it now, e.g. before the user signs out.
     *
     * Waits for an upload already in progress, so everything recorded before the call has been sent when it returns.
     * Throws what `upload` throws; the records stay saved and are sent by a later upload.
     */
    public suspend fun flush() {
        outbox.persist()
        outbox.flush()
    }

    internal suspend fun instrumentFetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
    ): RequestResponse {
        if (currentCoroutineContext()[TelemetryUploading] != null) return proceed(url, method, headers, body)

        // Claude: `user:pass@` can only appear in the authority, so taking the host after its last '@' drops credentials,
        // and the path is read after the authority. A relative URL has no authority: it goes to the page's own origin.
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' }
        val hostAndPort = authority.substringAfterLast('@')
        val host = (if (hostAndPort.startsWith("[")) hostAndPort.substringBefore("]") + "]" else hostAndPort.substringBefore(":")).lowercase()
        val path = (if (afterScheme.isEmpty()) url else afterScheme.drop(authority.length))
            .substringBefore("?").substringBefore("#") // queries carry tokens
        val attributes = TelemetryAttributes {
            put(TelemetryKeys.Http.requestMethod, method.name)
            put(TelemetryKeys.Url.path, if (path.startsWith("/")) path else "/$path")
            if (host.isNotEmpty()) put(TelemetryKeys.Server.address, host)
        }
        // Claude: a same-origin request goes to the app's own server, which is where propagation is wanted.
        val propagate = if (host.isEmpty()) tracePropagationHosts.isNotEmpty() else tracePropagationHosts.any { host == it.lowercase() || host.endsWith(".${it.lowercase()}") }
        return span("http", method.name, attributes) { span ->
            val outHeaders = headers.copy()
            if (propagate) {
                outHeaders.set("traceparent", "00-${span.traceId}-${span.spanId}-${if (sampled) "01" else "00"}")
            }
            val response = proceed(url, method, outHeaders, body)
            span.attributes += telemetryAttributesOf(TelemetryKeys.Http.responseStatusCode to response.status.toLong())
            span.failed = response.status >= 400 // OpenTelemetry's rule for client spans
            response
        }
    }

    /** Runs for as long as a screen is shown, with the screen's attributes in the coroutine context. */
    internal suspend fun recordScreenVisit(navigator: PageNavigator, load: ScreenLoad) {
        val screenAttributes = currentCoroutineContext()[TelemetryAttributesElement]?.attributes ?: TelemetryAttributes.empty
        if (navigator === mainNavigator) currentScreen = screenAttributes
        event("app.screen.view_start", screenAttributes)
        try {
            try {
                span("screen", "load") {
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

        var installed: Telemetry? = null

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

/**
 * Records [action] as span `owner.operation` with the installed [Telemetry], as a child of the current span if
 * there is one. Just runs [action] when telemetry isn't installed.
 */
public suspend fun <T> span(
    owner: String,
    operation: String,
    attributes: TelemetryAttributes = TelemetryAttributes.empty,
    action: suspend () -> T,
): T {
    val telemetry = Telemetry.installed ?: return action()
    return telemetry.span(owner, operation, attributes) { action() }
}

/** The span the current coroutine runs in; spans started inside it become its children. */
internal class ClientSpan(val traceId: String, val spanId: String, var attributes: TelemetryAttributes) : AbstractCoroutineContextElement(Key) {
    /** Set when the work returned normally but still failed, such as a request answered with an error status. */
    var failed: Boolean = false

    companion object Key : CoroutineContext.Key<ClientSpan>
}

/** Attributes for everything recorded in this coroutine; elements carry their [ElementContext.telemetryAttributes]. */
internal class TelemetryAttributesElement(val attributes: TelemetryAttributes) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TelemetryAttributesElement>
}

private fun Throwable.toRecordedError(): RecordedError = RecordedError(
    type = this::class.simpleName ?: "Unknown",
    message = message ?: "",
    stacktrace = stackTraceToString(),
    fingerprint = CrashFingerprint.generate(this),
)
