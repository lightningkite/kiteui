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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


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

class ConnectivityGate(val clock: Clock = Clock.System, val delay: suspend (ms: Long) -> Unit = { ms -> kotlinx.coroutines.delay(ms) }) {
    val gate = WaitGate(true)
    val baseRetry = 10.seconds
    var nextRetry = baseRetry
    val maxRetry = 5.minutes
    val retryAt = Signal<Instant?>(null)

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

object Connectivity {
    val noConnectivityCodes = setOf<Short>(502, 503)
    val tooMuchCodes = setOf<Short>(420, 429)
    val stopConnectivityCodes = noConnectivityCodes + tooMuchCodes
    val fetchGate = ConnectivityGate()
    val lastConnectivityIssueCode: Signal<Short> = Signal(0)
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
                Log.warn("Forced retry on $method $url")
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