package com.lightningkite.kiteui

import com.lightningkite.reactive.core.Release
import kotlin.experimental.ExperimentalTypeInference

/** Reports transfer progress. `bytesExpected` is -1 when the total size is not known in advance. */
public typealias ProgressCallback = (bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit

/**
 * Every request the app makes to the network, behind one interface.
 *
 * Exists so that the things which decorate network access — telemetry, authentication, and the fakes
 * a test substitutes for them — compose as ordinary objects rather than accumulating in global
 * state. Each concern holds the next [HttpFetcher]:
 *
 * ```kotlin
 * val fetcher = HttpFetcher.Platform
 *     .intercepted(telemetryInterceptor)
 *     .withHeaders { listOf("Authorization" to token()) }
 * ```
 *
 * Every layer here is a genuine transformation of one request into one request, which is what makes
 * the order between them a matter of intent rather than of correctness. Retry is not such a layer
 * and is deliberately absent: it re-runs an operation rather than transforming it, so it lives in
 * [ConnectivityGate.runRequest], which takes the operation as a lambda and can therefore say what
 * gets re-run.
 *
 * A test builds a chain over a fake instead of the platform, which is what makes connectivity
 * failures reproducible; the previous arrangement — a top-level `fetch` reading a global interceptor
 * list — could only be faked by mutating process-wide state, so tests hand-copied the stack instead.
 *
 * [webSocket] belongs here alongside [fetch] because a test that can fake requests but not sockets
 * cannot exercise a reconnect.
 */
public interface HttpFetcher {
    public suspend fun fetch(
        url: String,
        method: HttpMethod = HttpMethod.GET,
        headers: HttpHeaders = HttpHeaders(),
        body: RequestBody? = null,
        onUploadProgress: ProgressCallback? = null,
        onDownloadProgress: ProgressCallback? = null,
    ): RequestResponse

    /** The socket is not opened until it is used; see [retryWebSocket] for the reconnecting form. */
    public fun webSocket(url: String): WebSocket

    /** Wraps HTTP requests. Call [proceed] to continue the chain. */
    public fun interface Interceptor {
        public suspend fun intercept(
            url: String,
            method: HttpMethod,
            headers: HttpHeaders,
            body: RequestBody?,
            proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
        ): RequestResponse

        public companion object {
            private val _registered = ArrayList<Interceptor>()

            /** Every interceptor the top-level [fetch] applies, first registered outermost. A view that changes as interceptors are registered and released, so copy it before iterating. */
            public val registered: List<Interceptor> = _registered

            /** Adds [interceptor] to every top-level [fetch], and returns a [Release] that removes it. */
            public fun register(interceptor: Interceptor): Release {
                _registered += interceptor
                return { _registered.remove(interceptor) }
            }
        }
    }

    /**
     * Runs [interceptors] around each request of [wraps], the first listed being the outermost.
     *
     * [interceptors] is read on every request, so a list that changes, such as [Interceptor.registered], takes effect immediately.
     */
    public class Intercepted(private val wraps: HttpFetcher, private val interceptors: List<Interceptor>) : HttpFetcher {
        override suspend fun fetch(
            url: String,
            method: HttpMethod,
            headers: HttpHeaders,
            body: RequestBody?,
            onUploadProgress: ProgressCallback?,
            onDownloadProgress: ProgressCallback?,
        ): RequestResponse {
            var proceed: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse =
                { u, m, h, b -> wraps.fetch(u, m, h, b, onUploadProgress, onDownloadProgress) }
            for (interceptor in interceptors.toList().asReversed()) {
                val next = proceed
                proceed = { u, m, h, b -> interceptor.intercept(u, m, h, b, next) }
            }
            return proceed(url, method, headers, body)
        }

        override fun webSocket(url: String): WebSocket = wraps.webSocket(url)
    }

    /** The platform's own networking, with nothing wrapped around it. The base of every chain. */
    public object Platform : HttpFetcher by PlatformHttpFetcher

    /**
     * What the top-level [fetch] uses: [Platform] wrapped in the registered [Interceptor]s.
     *
     * Present so that code which has not yet been handed an [HttpFetcher] keeps behaving as it
     * did. New code should take an [HttpFetcher] parameter instead of reaching for this, so that
     * a test can supply its own.
     */
    public object Default : HttpFetcher by Intercepted(PlatformHttpFetcher, Interceptor.registered)
}

internal expect object PlatformHttpFetcher : HttpFetcher {
    override suspend fun fetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        onUploadProgress: ProgressCallback?,
        onDownloadProgress: ProgressCallback?
    ): RequestResponse

    override fun webSocket(url: String): WebSocket
}

/**
 * Returns an [HttpFetcher] that runs [interceptors] around each request of this one, the first
 * listed being the outermost.
 */
public fun HttpFetcher.intercepted(interceptors: List<HttpFetcher.Interceptor>): HttpFetcher =
    HttpFetcher.Intercepted(this, interceptors)

/**
 * Returns an [HttpFetcher] that adds the headers [calculator] produces to each request, overriding
 * any of the same name the caller supplied.
 *
 * [calculator] runs per request rather than once, so a fetcher built this way stays correct however
 * long it is held onto and wherever in a chain it sits — including inside a
 * [ConnectivityGate.runRequest] lambda, where each retry calls [fetch] again and so recomputes.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@Deprecated("Return HttpHeaders explicitly", ReplaceWith("withHeaders { HttpHeaders(calculator()) }"))
public fun HttpFetcher.withHeaders(calculator: suspend () -> List<Pair<String, String>>): HttpFetcher =
    this.withHeaders { HttpHeaders(calculator()) }

/**
 * Returns an [HttpFetcher] that adds the headers [calculator] produces to each request, overriding
 * any of the same name the caller supplied.
 *
 * [calculator] runs per request rather than once, so a fetcher built this way stays correct however
 * long it is held onto and wherever in a chain it sits — including inside a
 * [ConnectivityGate.runRequest] lambda, where each retry calls [fetch] again and so recomputes.
 */
public fun HttpFetcher.withHeaders(calculator: suspend () -> HttpHeaders): HttpFetcher =
    HeaderCalculatingHttpFetcher(this, calculator)

private class HeaderCalculatingHttpFetcher(
    private val wrapped: HttpFetcher,
    private val calculator: suspend () -> HttpHeaders
) : HttpFetcher {
    override suspend fun fetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        onUploadProgress: ProgressCallback?,
        onDownloadProgress: ProgressCallback?,
    ): RequestResponse = wrapped.fetch(
        url = url,
        method = method,
        headers = headers + calculator(),
        body = body,
        onUploadProgress = onUploadProgress,
        onDownloadProgress = onDownloadProgress,
    )

    override fun webSocket(url: String): WebSocket = wrapped.webSocket(url)
}
