package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.shared
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


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
        else return suspendCoroutineCancellable {
            continuations.add(it)
            return@suspendCoroutineCancellable {}
        }
    }
    fun abandon() {
        for (continuation in continuations) {
            continuation.resumeWithException(CancelledException("abandoned as requested"))
        }
        continuations.clear()
    }
}

class ConnectivityGate(val clock: Clock = Clock.System, val delay: suspend (ms: Long) -> Unit = { ms -> kotlinx.coroutines.delay(ms) }) {
    val gate = WaitGate(true)
    val baseRetry = 10.seconds
    var nextRetry = baseRetry
    val maxRetry = 5.minutes
    val retryAt = Property<Instant?>(null)

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
                if (retryAt.value == null) {
                    launchGlobal {
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
    val lastConnectivityIssueCode: Property<Short> = Property(0)
}

suspend fun connectivityFetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: suspend () -> HttpHeaders = { httpHeaders() },
    body: RequestBody,
): RequestResponse {
    return if(coroutineContext[ConnectivityIssueSuppress.Key] == null) {
        Connectivity.fetchGate.run("$method $url") {
            val r = try {
                fetch(url = url, method = method, headers = headers(), body = body)
            } catch(e: ConnectionException) {
                // Perform a single retry immediately
                println("WARNING: Forced retry on $method $url")
                try {
                    fetch(url = url, method = method, headers = headers(), body = body)
                } catch(e: ConnectionException) {
                    Connectivity.lastConnectivityIssueCode.value = 0
                    throw e
                }
                throw e
            }
            if (r.status in Connectivity.stopConnectivityCodes) {
                Connectivity.lastConnectivityIssueCode.value = r.status
                throw ConnectionException("Status code ${r.status}")
            }
            r
        }
    } else {
        fetch(url = url, method = method, headers = headers(), body = body)
    }
}

class ConnectivityIssueSuppress(): CoroutineContext.Element {
    override val key: CoroutineContext.Key<ConnectivityIssueSuppress> = Key
    object Key: CoroutineContext.Key<ConnectivityIssueSuppress>
}
suspend fun <T> suppressConnectivityIssues(action: suspend () -> T): T {
    val child = coroutineContext + ConnectivityIssueSuppress()
    return suspendCoroutine<T> {
        action.startCoroutine(it)
    }
}