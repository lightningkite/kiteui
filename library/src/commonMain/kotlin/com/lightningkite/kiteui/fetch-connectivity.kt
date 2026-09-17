package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant


public class WaitGate(permit: Boolean = false) {
    public var permit: Boolean = permit
        set(value) {
            field = value
            if (value) {
                for (continuation in continuations) {
                    continuation.resume(Unit)
                }
                continuations.clear()
            }
        }
    public fun permitOnce() {
        permit = true
        permit = false
    }
    public val continuations: MutableList<Continuation<Unit>> = ArrayList<Continuation<Unit>>()
    public suspend fun await(): Unit {
        if (permit) return
        else return suspendCancellableCoroutine {
            continuations.add(it)
            it.invokeOnCancellation { _ -> continuations.remove(it) }
        }
    }
    public fun abandon() {
        for (continuation in continuations) {
            continuation.resumeWithException(CancellationException("abandoned as requested"))
        }
        continuations.clear()
    }
}

public class ConnectivityGate(
    public val clock: Clock = Clock.System,
    public val delay: suspend (ms: Long) -> Unit = { ms -> kotlinx.coroutines.delay(ms) },
    public val random: Random = Random.Default,
    /**
     * Where the retry timer runs. Resolved per use rather than at construction so that building a
     * gate does not force [AppScope] to initialize, which a test target lacking a main dispatcher
     * cannot do.
     */
    private val scope: () -> CoroutineScope = { AppScope },
) {
    public val gate: WaitGate = WaitGate(true)
    public val baseRetry: Duration = 10.seconds
    public val maxRetry: Duration = 5.minutes

    /**
     * Ceiling for the next self-scheduled wait, doubling per consecutive failure. The wait actually
     * taken is a random point below this rather than the value itself, so it is not a delay.
     */
    public var nextRetry: Duration = baseRetry

    public val retryAt: Signal<Instant?> = Signal<Instant?>(null)

    /**
     * Invalidates a sleeping timer once something has superseded it. Without this a timer armed
     * before a [retryNow] would wake afterwards and lift a newer, unrelated wait early.
     */
    private var scheduleGeneration: Int = 0

    /**
     * Retries at once and returns the schedule to [baseRetry].
     *
     * For recovery the app has actual evidence of: the device reporting itself back online, the app
     * returning to the foreground, or the user asking. Resetting matters as much as releasing —
     * retrying on an escalated interval is why callers took to invoking this twice in a row hoping
     * one of them landed. Evidence that connectivity returned says more about the next attempt than
     * the count of failures before it.
     *
     * Deliberately unjittered. These triggers fire independently on each device, so there is no herd
     * to spread; and if the attempt fails anyway, the jittered backoff takes over again from base,
     * which bounds what a misleading signal can cost.
     */
    public fun retryNow() {
        nextRetry = baseRetry
        scheduleGeneration++
        release()
    }

    public fun abandon() {
        scheduleGeneration++
        gate.abandon()
        release()
    }

    private fun release() {
        retryAt.value = null
        gate.permit = true
    }

    /**
     * Runs [action], retrying with backoff for as long as it reports a [ConnectionException].
     *
     * A [RequestBlockedException] is not caught: the platform will refuse that request identically
     * on every attempt, so it propagates to the caller instead of entering a retry loop that can
     * never succeed.
     */
    public suspend fun <T> run(tag: String, action: suspend () -> T): T {
        while (true) {
            gate.await()
            try {
                val r = action()
                nextRetry = baseRetry
                return r
            } catch (e: ConnectionException) {
                e.printStackTrace2()
                if (retryAt.value == null) scheduleRetry(e)
            }
        }
    }

    /**
     * Closes the gate and arms a timer to reopen it.
     *
     * The wait is a uniform random point in `[0, nextRetry]` — full jitter — rather than `nextRetry`
     * itself. The failures worth backing off from are the ones a server causes, and those strike
     * every client at the same instant: a deploy or a crash fails them all together, so an exact
     * schedule has the whole fleet return in lockstep, spike the recovering server, fail again, and
     * re-synchronize. Randomizing spreads that return across the interval. The ceiling still doubles
     * per failure, so the request rate falls the way it should; only the arrival times scatter.
     */
    private fun scheduleRetry(cause: ConnectionException) {
        val ceiling = nextRetry
        val wait = when (val after = (cause as? RateLimitedException)?.retryAfter) {
            null -> ceiling.times(random.nextDouble())
            // Never earlier than the server asked for, but not to the exact second either: it handed
            // every client it throttled the same deadline, so obeying it precisely rebuilds the spike
            // the header exists to prevent.
            else -> after + baseRetry.times(random.nextDouble())
        }
        val generation = ++scheduleGeneration
        retryAt.value = clock.now() + wait
        nextRetry = ceiling.times(2).coerceAtMost(maxRetry)
        gate.permit = false
        scope().launch {
            delay(wait.inWholeMilliseconds)
            if (generation != scheduleGeneration) return@launch
            release()
        }
    }
}

@Deprecated("Use Connectivity instead", ReplaceWith("Connectivity.fetchGate", "com.lightningkite.kiteui.Connectivity"))
public val connectivityFetchGate: ConnectivityGate get() = Connectivity.fetchGate

public object Connectivity {
    public val noConnectivityCodes: Set<Short> = setOf<Short>(502, 503)
    public val tooMuchCodes: Set<Short> = setOf<Short>(420, 429)
    public val stopConnectivityCodes: Set<Short> = noConnectivityCodes + tooMuchCodes
    public val fetchGate: ConnectivityGate = ConnectivityGate()
    public val lastConnectivityIssueCode: Signal<Short> = Signal(0)
}

/**
 * Which retryable failure a discouraging status code represents, so [ConnectivityGate] can schedule
 * the retry to suit the cause rather than treating every one of them as an unreachable server.
 */
internal fun RequestResponse.connectivityFailure(): ConnectionException =
    if (status in Connectivity.tooMuchCodes) RateLimitedException("Status code $status", retryAfter = retryAfter())
    else ServerUnavailableException("Status code $status")

/**
 * The server's `Retry-After`, in the delta-seconds form.
 *
 * The header also permits an HTTP-date, which rate limiters in practice do not send and which cannot
 * be parsed here without a date library. That form, and any malformed value, reads as absent and
 * leaves the gate to compute its own backoff.
 */
private fun RequestResponse.retryAfter(): Duration? =
    headers.get("Retry-After")?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.seconds

/** Whether a server answered at all. Governs both whether retrying instantly could help and whether there is a status code to report. */
private val ConnectionException.serverAnswered: Boolean
    get() = this is ServerUnavailableException || this is RateLimitedException

/**
 * Retries a whole request under this gate, reading discouraging status codes as the retryable
 * failures they represent.
 *
 * [request] produces a complete response and the gate re-invokes it in full, so everything a fresh
 * attempt needs belongs inside the lambda — recomputed authentication headers above all, since a
 * token can expire while the gate is waiting out a five-minute backoff.
 *
 * Deliberately not an [HttpFetcher] decorator. Retrying is not a transformation of a request, it is
 * a scope that re-runs one, and a decorator chain cannot say which. Given a uniform interface,
 * `platform.withHeaders(..).retrying(gate)` and `platform.retrying(gate).withHeaders(..)` have the
 * same type and both compile, yet only the first recomputes headers per attempt; the second replays
 * a stale token for as long as the gate keeps trying. A lambda has no such pair to confuse: the
 * scope is the braces, and what is re-run is what you can see inside them.
 */
public suspend fun ConnectivityGate.runRequest(
    tag: String,
    request: suspend () -> RequestResponse,
): RequestResponse {
    suspend fun attempt(): RequestResponse {
        val response = request()
        if (response.status in Connectivity.stopConnectivityCodes) {
            Connectivity.lastConnectivityIssueCode.value = response.status
            throw response.connectivityFailure()
        }
        return response
    }
    if (coroutineContext[ConnectivityIssueSuppress.Key] != null) return attempt()
    return run(tag) {
        try {
            attempt()
        } catch (e: ConnectionException) {
            // One immediate retry absorbs a transport blip - a dropped keep-alive, a DNS miss -
            // without making the user sit out a whole backoff interval. Deliberately skipped once a
            // server has answered: it is reachable and already declining, so asking again this
            // instant only doubles the load it is failing to carry.
            if (e.serverAnswered) throw e
            Log.warn("Immediate retry on $tag after ${e.message}")
            try {
                attempt()
            } catch (again: ConnectionException) {
                // Nothing answered, so there is no status code to describe the problem with.
                if (!again.serverAnswered) Connectivity.lastConnectivityIssueCode.value = 0
                throw again
            }
        }
    }
}

/**
 * Issues a request through the process-wide chain, retrying under [Connectivity.fetchGate].
 *
 * Prefer [ConnectivityGate.runRequest] over an [HttpFetcher] that was handed to you. This function
 * reaches only the one global gate, so a test cannot substitute a gate with a controllable clock,
 * and every caller shares a single backoff schedule.
 */
public suspend fun connectivityFetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: suspend () -> HttpHeaders = { httpHeaders() },
    body: RequestBody?,
): RequestResponse = Connectivity.fetchGate.runRequest("$method $url") {
    // headers() is called in here, not outside, so every retry carries a freshly computed set.
    HttpFetcher.default.fetch(url = url, method = method, headers = headers(), body = body)
}

public class ConnectivityIssueSuppress(): CoroutineContext.Element {
    override val key: CoroutineContext.Key<ConnectivityIssueSuppress> = Key
    public object Key: CoroutineContext.Key<ConnectivityIssueSuppress>
}
public suspend fun <T> suppressConnectivityIssues(action: suspend () -> T): T {
    return withContext(ConnectivityIssueSuppress()) { action() }
}
