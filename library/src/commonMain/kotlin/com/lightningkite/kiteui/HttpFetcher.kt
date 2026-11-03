package com.lightningkite.kiteui

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
 * val fetcher = HttpFetcher.platform
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
        headers: HttpHeaders = httpHeaders(),
        body: RequestBody? = null,
        onUploadProgress: ProgressCallback? = null,
        onDownloadProgress: ProgressCallback? = null,
    ): RequestResponse

    /** The socket is not opened until it is used; see [retryWebSocket] for the reconnecting form. */
    public fun webSocket(url: String): WebSocket

    public companion object {
        /**
         * The platform's own networking, with nothing wrapped around it. The base of every chain.
         */
        public val platform: HttpFetcher = PlatformHttpFetcher

        /**
         * What the top-level [fetch] uses: [platform] plus whatever is in the global
         * [fetchInterceptors] at the time of the call.
         *
         * Present so that code which has not yet been handed an [HttpFetcher] keeps behaving as it
         * did. New code should take an [HttpFetcher] parameter instead of reaching for this, so that
         * a test can supply its own.
         */
        public val default: HttpFetcher = GlobalInterceptorHttpFetcher(platform)
    }
}

/** Delegates straight to the platform's `fetchRaw` and `webSocket`. */
private object PlatformHttpFetcher : HttpFetcher {
    override suspend fun fetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        onUploadProgress: ProgressCallback?,
        onDownloadProgress: ProgressCallback?,
    ): RequestResponse = platformFetch(url, method, headers, body, onUploadProgress, onDownloadProgress)

    // Qualified because the member would otherwise shadow the top-level expect function.
    override fun webSocket(url: String): WebSocket = platformWebSocket(url)
}

/**
 * Applies the process-wide [fetchInterceptors] to each request, reading the list per call so that
 * interceptors installed and removed at runtime — as [com.lightningkite.kiteui.telemetry.Telemetry]
 * does — take effect immediately.
 *
 * Only here to preserve the behavior of the top-level [fetch]. Prefer [intercepted], which composes
 * a fixed chain that a test can construct independently.
 */
private class GlobalInterceptorHttpFetcher(private val wrapped: HttpFetcher) : HttpFetcher {
    override suspend fun fetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        onUploadProgress: ProgressCallback?,
        onDownloadProgress: ProgressCallback?,
    ): RequestResponse = applyInterceptors(
        interceptors = fetchInterceptors.toList(),
        url = url,
        method = method,
        headers = headers,
        body = body,
    ) { u, m, h, b -> wrapped.fetch(u, m, h, b, onUploadProgress, onDownloadProgress) }

    override fun webSocket(url: String): WebSocket = wrapped.webSocket(url)
}

/** Wraps each request in [interceptors], outermost first. */
private class InterceptedHttpFetcher(
    private val wrapped: HttpFetcher,
    private val interceptors: List<FetchInterceptor>,
) : HttpFetcher {
    override suspend fun fetch(
        url: String,
        method: HttpMethod,
        headers: HttpHeaders,
        body: RequestBody?,
        onUploadProgress: ProgressCallback?,
        onDownloadProgress: ProgressCallback?,
    ): RequestResponse = applyInterceptors(
        interceptors = interceptors,
        url = url,
        method = method,
        headers = headers,
        body = body,
    ) { u, m, h, b -> wrapped.fetch(u, m, h, b, onUploadProgress, onDownloadProgress) }

    override fun webSocket(url: String): WebSocket = wrapped.webSocket(url)
}

/**
 * Returns an [HttpFetcher] that runs [interceptors] around each request of this one, the first
 * listed being the outermost.
 */
public fun HttpFetcher.intercepted(vararg interceptors: FetchInterceptor): HttpFetcher =
    InterceptedHttpFetcher(this, interceptors.toList())

/**
 * Returns an [HttpFetcher] that adds the headers [calculator] produces to each request, overriding
 * any of the same name the caller supplied.
 *
 * [calculator] runs per request rather than once, so a fetcher built this way stays correct however
 * long it is held onto and wherever in a chain it sits — including inside a
 * [ConnectivityGate.runRequest] lambda, where each retry calls [fetch] again and so recomputes.
 */
public fun HttpFetcher.withHeaders(calculator: suspend () -> List<Pair<String, String>>): HttpFetcher =
    HeaderCalculatingHttpFetcher(this, calculator)

private class HeaderCalculatingHttpFetcher(
    private val wrapped: HttpFetcher,
    private val calculator: suspend () -> List<Pair<String, String>>,
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
        headers = httpHeaders(headers).also { combined ->
            for ((name, value) in calculator()) combined.set(name, value)
        },
        body = body,
        onUploadProgress = onUploadProgress,
        onDownloadProgress = onDownloadProgress,
    )

    override fun webSocket(url: String): WebSocket = wrapped.webSocket(url)
}

/** Builds the interceptor chain and invokes it. Shared by the two interceptor-applying fetchers. */
private suspend fun applyInterceptors(
    interceptors: List<FetchInterceptor>,
    url: String,
    method: HttpMethod,
    headers: HttpHeaders,
    body: RequestBody?,
    terminal: suspend (String, HttpMethod, HttpHeaders, RequestBody?) -> RequestResponse,
): RequestResponse {
    var proceed = terminal
    for (interceptor in interceptors.asReversed()) {
        val next = proceed
        proceed = { u, m, h, b -> interceptor(u, m, h, b, next) }
    }
    return proceed(url, method, headers, body)
}
