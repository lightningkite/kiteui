package com.lightningkite.kiteui

import com.lightningkite.signal.AppScope
import com.lightningkite.signal.Property
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


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
    public val continuations: ArrayList<Continuation<Unit>> = ArrayList<Continuation<Unit>>()
    public suspend fun await(): Unit {
        if (permit) return
        else return suspendCancellableCoroutine {
            continuations.add(it)
        }
    }
    public fun abandon() {
        for (continuation in continuations) {
            continuation.resumeWithException(CancellationException("abandoned as requested"))
        }
        continuations.clear()
    }
}

public class ConnectivityGate(public val clock: Clock = Clock.System, public val delay: suspend (ms: Long) -> Unit = { ms -> kotlinx.coroutines.delay(ms) }) {
    public val gate: WaitGate = WaitGate(true)
    public val baseRetry: Duration = 10.seconds
    public var nextRetry: Duration = baseRetry
    public val maxRetry: Duration = 5.minutes
    public val retryAt: Property<Instant?> = Property<Instant?>(null)

    public fun retryNow() {
        retryAt.value = null
        gate.permit = true
    }

    public fun abandon() {
        gate.abandon()
        retryAt.value = null
        gate.permit = true
    }

    public suspend fun <T> run(tag: String, action: suspend () -> T): T {
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
public val connectivityFetchGate: ConnectivityGate get() = Connectivity.fetchGate

public object Connectivity {
    public val noConnectivityCodes: Set<Short> = setOf<Short>(502, 503)
    public val tooMuchCodes: Set<Short> = setOf<Short>(420, 429)
    public val stopConnectivityCodes: Set<Short> = noConnectivityCodes + tooMuchCodes
    public val fetchGate: ConnectivityGate = ConnectivityGate()
    public val lastConnectivityIssueCode: Property<Short> = Property(0)
}

public suspend fun connectivityFetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: suspend () -> HttpHeaders = { httpHeaders() },
    body: RequestBody?,
): RequestResponse {
    return if(coroutineContext[ConnectivityIssueSuppress.Key] == null) {
        Connectivity.fetchGate.run("$method $url") {
            try {
                fetch(url = url, method = method, headers = headers(), body = body)
            } catch(e: ConnectionException) {
                // Perform a single retry immediately
                println("WARNING: Forced retry on $method $url")
                val r = try {
                    fetch(url = url, method = method, headers = headers(), body = body)
                } catch(e: ConnectionException) {
                    Connectivity.lastConnectivityIssueCode.value = 0
                    throw e
                }
                if (r.status in Connectivity.stopConnectivityCodes) {
                    Connectivity.lastConnectivityIssueCode.value = r.status
                    throw ConnectionException("Status code ${r.status}")
                }
                r
            }
        }
    } else {
        fetch(url = url, method = method, headers = headers(), body = body)
    }
}

public class ConnectivityIssueSuppress(): CoroutineContext.Element {
    public override val key: CoroutineContext.Key<ConnectivityIssueSuppress> = Key
    public object Key: CoroutineContext.Key<ConnectivityIssueSuppress>
}
public suspend fun <T> suppressConnectivityIssues(action: suspend () -> T): T {
    return withContext(ConnectivityIssueSuppress()) { action() }
}