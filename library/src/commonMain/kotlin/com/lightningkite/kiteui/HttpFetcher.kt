package com.lightningkite.kiteui

import com.lightningkite.kiteui.utils.LiveRegistry
import com.lightningkite.reactive.core.Release
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

/** Reports transfer progress. `bytesExpected` is -1 when the total size is not known in advance. */
public typealias ProgressCallback = (bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit

/**
 * Makes HTTP requests and opens WebSockets.
 *
 * Wrap a fetcher to add behavior to every request, such as authentication, and take an [HttpFetcher] parameter
 * instead of calling the top-level [fetch], so a test can pass a fake:
 *
 * ```kotlin
 * val fetcher = HttpFetcher.Platform.withHeaders { HttpHeaders("Authorization" to "Bearer ${token()}") }
 * ```
 *
 * Retrying is not a wrapper; use [ConnectivityGate.runRequest], which takes the request as a lambda and so can
 * run it again.
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
            /** Every interceptor the top-level [fetch] applies, first registered outermost. */
            public val global: LiveRegistry<Interceptor> = LiveRegistry()
        }
    }

    /**
     * Runs [interceptors] around each request of [wraps], the first listed being the outermost.
     *
     * [interceptors] is read on every request, so registrations and releases in a registry such as [Interceptor.global] take effect immediately.
     */
    public class Intercepted(private val wraps: HttpFetcher, private val interceptors: LiveRegistry<Interceptor>) : HttpFetcher {
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
            for (interceptor in interceptors.snapshot().asReversed()) {
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
    public companion object Default : HttpFetcher by Intercepted(PlatformHttpFetcher, Interceptor.global)
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
    HttpFetcher.Intercepted(this, LiveRegistry(interceptors))

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
@JvmName("withHeadersFromPairs")
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
