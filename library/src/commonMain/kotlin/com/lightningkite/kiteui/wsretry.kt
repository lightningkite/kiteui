package com.lightningkite.kiteui

import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

// by Claude — Bug 9: removed artificial 100ms delay; resume immediately on connect
public suspend fun WebSocket.waitUntilConnect(delay: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }) {
    suspendCancellableCoroutine<Unit> {
        var alreadyResumed = false
        onOpen {
            if (!alreadyResumed) {
                alreadyResumed = true
                it.resume(Unit)
            }
        }
        onClose { code ->
            if (!alreadyResumed) {
                alreadyResumed = true
                it.resumeWithException(ConnectionException("Socket closed almost immediately.  Code $code"))
            }
        }
    }
}

@Deprecated("Use retryWebSocket, the proper spelling", ReplaceWith("retryWebSocket"))
public fun retryWebsocket(
    url: String,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebSocket = retryWebSocket(
    underlyingSocket = { webSocket(url) },
    pingTime = pingTime,
    gate = gate,
    log = log
)

@Deprecated("Use retryWebSocket, the proper spelling", ReplaceWith("retryWebSocket"))
public fun retryWebsocket(
    underlyingSocket: suspend () -> WebSocket,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebSocket = retryWebSocket(
    underlyingSocket = underlyingSocket,
    pingTime = pingTime,
    gate = gate,
    log = log
)

public fun retryWebSocket(
    url: String,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebSocket = retryWebSocket(
    underlyingSocket = { webSocket(url) },
    pingTime = pingTime,
    gate = gate,
    log = log
)

/**
 * The socket this wrapper is currently using, and the id that marks it as the current one.
 *
 * An object with volatile fields rather than captured `var`s, because the threads that touch these
 * are genuinely different: [retryWebSocket]'s connection attempt runs on whatever dispatcher its
 * scope supplies - a pool thread on Android and iOS - while every callback guard reads [id] from the
 * platform's callback thread, which both of those platforms marshal to main.  A plain local gives no
 * visibility guarantee across that boundary, and the write that would be missed is the one that says
 * which socket is current: miss it and a live socket's close is discarded as stale, leaving
 * `connected` true for a socket that has already gone.
 *
 * Visibility is all this needs.  Only one attempt runs at a time, so [nextId] is never incremented
 * concurrently with itself.
 */
private class CurrentSocket {
    @Volatile var socket: WebSocket? = null
    @Volatile var id: Int = -1
    @Volatile var nextId: Int = 0
}

public fun retryWebSocket(
    underlyingSocket: suspend () -> WebSocket,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebSocket {
    log?.log("Creating")
    var lastConnect = 0.0
    val connectedSignal = Signal(false).also {
        it.addListener {
            log?.log("connected: ${it.value}")
        }
    }
    val current = CurrentSocket()
    val onOpenList = ArrayList<() -> Unit>()
    val onMessageList = ArrayList<(String) -> Unit>()
    val onBinaryMessageList = ArrayList<(Blob) -> Unit>()
    val onCloseList = ArrayList<(Short) -> Unit>()
    var lastPong = clockMillis()
    suspend fun reset() {
        val id = current.nextId++
        current.id = id
        current.socket?.close(1000, "Reconnecting")
        current.socket = underlyingSocket().also { socket ->
            var pings: Job? = null
            socket.onOpen {
                if (id != current.id) return@onOpen
                log?.log("$id onOpen")
                onOpenList.toList().forEach { l -> l() }
            }
            socket.onMessage {
                if (id != current.id) return@onMessage
                log?.log("$id onMessage $it")
                lastPong = clockMillis()
                if (it.isNotBlank()) onMessageList.toList().forEach { l -> l(it) }
            }
            socket.onBinaryMessage {
                if (id != current.id) return@onBinaryMessage
                log?.log("$id onBinaryMessage $it")
                onBinaryMessageList.toList().forEach { l -> l(it) }
            }
            socket.onClose {
                if (id != current.id) return@onClose
                log?.log("$id onClose $it")
                onCloseList.toList().forEach { l -> l(it) }
            }
            socket.onOpen {
                if (id != current.id) return@onOpen
                lastConnect = clockMillis()
                lastPong = lastConnect
                connectedSignal.value = true
                pings?.cancel()
                pings = AppScope.launch {
                    while (true) {
                        delay(pingTime)
                        if (id != current.id) return@launch
                        val now = clockMillis()
                        when {
                            // by Claude — Bug 10: exit ping loop after sending close
                            lastPong < now - (pingTime * 3) -> {
                                socket.close(
                                    3000,
                                    "Server did not respond to three consecutive pings."
                                )
                                return@launch
                            }

                            lastPong < now - pingTime.times(0.8) -> socket.send(" ")
                        }
                    }
                }
            }
            socket.onClose {
                if (id != current.id) return@onClose
                pings?.cancel()
                connectedSignal.value = false
            }
        }
    }

    return object : RetryWebSocket, CoroutineScope {

        override val connected: Reactive<Boolean>
            get() = connectedSignal
        var listenerCounter = 0
        val shouldBeOn = Signal(false)

        override fun beginUse(): () -> Unit {
            if (listenerCounter++ == 0) shouldBeOn.value = true
            return {
                if (--listenerCounter == 0) shouldBeOn.value = false
            }
        }

        override val coroutineContext: CoroutineContext = SupervisorJob()

        /**
         * One attempt at getting a live socket, reporting every way it can fail as a
         * [ConnectionException] so [ConnectivityGate] backs it off and tries again.
         *
         * Everything here is a failure to connect, whoever it came from: a header calculation that
         * threw, a URL the platform would not take.  Letting any of them escape as themselves ends
         * the retrying, and a socket that has stopped retrying never comes back.
         */
        suspend fun connectOnce() {
            try {
                reset()
                current.socket?.waitUntilConnect(gate.delay)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConnectionException) {
                throw e
            } catch (e: RequestBlockedException) {
                // The platform refuses this identically every time, so retrying cannot help; it is
                // the one failure worth giving up on.
                throw e
            } catch (e: Exception) {
                throw ConnectionException("Could not open the socket: ${e.message}", e)
            }
        }

        init {
            /**
             * The attempt currently trying to get a socket up, if any.
             *
             * Deliberately a [Job] rather than a flag. The block below launches into its own
             * reactive context, whose coroutines are cancelled before every rerun, and a coroutine
             * cancelled before its body starts never runs a `finally` - so a flag cleared in one can
             * stay set forever, and the socket is then never retried again for the life of the app.
             * Asking the job whether it is still alive cannot be left behind that way: a superseded
             * attempt already reads as finished by the time this reruns, which both keeps two
             * attempts from running at once and picks the work back up after a cancellation.
             */
            var attempt: Job? = null
            reactive {
                val wanted = shouldBeOn()
                val isOn = connected()
                if (wanted && !isOn) {
                    if (attempt?.isActive != true) attempt = launch {
                        // ConnectivityGate.run retries a ConnectionException on its own schedule, so
                        // this loop is only for the ways out of it that are not failures to connect.
                        // ConnectivityGate.abandon() - which this app calls on every return to the
                        // foreground - delivers a CancellationException to an attempt that is
                        // otherwise perfectly healthy, and that one deserves another go; `isActive`
                        // is what tells it apart from being cancelled because this run was
                        // superseded or the scope is going away, where staying quiet is right.
                        // Deliberately no reads of `shouldBeOn` or `connectedSignal` here: this
                        // runs on whatever thread the scope supplies, those are written from the
                        // socket's callbacks on main, and a plain `var` read across threads can miss
                        // the write - taking another turn and resetting a socket that had just
                        // connected.  `isActive` is Job state, which is safe to read anywhere, and
                        // the explicit breaks below cover every other way out.
                        while (isActive) {
                            try {
                                log?.log("starting")
                                gate.run("WS") { connectOnce() }
                                log?.log("started A")
                                break
                            } catch (e: RequestBlockedException) {
                                log?.log("start blocked, not retrying: $e")
                                break
                            } catch (e: CancellationException) {
                                log?.log("start cancelled: $e")
                            } catch (e: Exception) {
                                // connectOnce reports every failure to connect as a
                                // ConnectionException, which the gate retries itself, so reaching
                                // here means something else went wrong: report it and stop rather
                                // than spin on it.
                                log?.log("start fail: $e")
                                e.printStackTrace2()
                                break
                            }
                        }
                    }
                } else if (!wanted && isOn) {
                    current.socket?.close(1000, "OK")
                }
            }
        }

        override fun close(code: Short, reason: String) {
            log?.log("close $code")
            val closing = current.socket
            current.socket = null
            current.id = -1
            closing?.close(code, reason)
            // Disowning the socket above also silences the onClose it is about to deliver, so the
            // closure has to be reported here.  Left unreported, `connected` stays true forever: the
            // loop above sees a live socket, never redials, and every send vanishes into the null
            // above.  A close while something still holds a use is a request to drop this
            // connection, not to stop - so saying so is also what lets it redial.
            if (connectedSignal.value) {
                connectedSignal.value = false
                onCloseList.toList().forEach { it(code) }
            }
        }

        override fun send(data: Blob) {
            log?.log("${current.id} send $data")
            current.socket?.send(data)
        }

        override fun send(data: String) {
            log?.log("${current.id} send $data")
            current.socket?.send(data)
        }

        override fun onOpen(action: () -> Unit) {
            onOpenList.add(action)
        }

        override fun onMessage(action: (String) -> Unit) {
            onMessageList.add(action)
        }

        override fun onBinaryMessage(action: (Blob) -> Unit) {
            onBinaryMessageList.add(action)
        }

        override fun onClose(action: (Short) -> Unit) {
            onCloseList.add(action)
        }
    }
}

public fun <SEND, RECEIVE> RetryWebSocket.typed(
    json: Json,
    send: KSerializer<SEND>,
    receive: KSerializer<RECEIVE>,
): TypedWebSocket<SEND, RECEIVE> = object : TypedWebSocket<SEND, RECEIVE> {
    override val connected: Reactive<Boolean>
        get() = this@typed.connected

    override fun beginUse(): () -> Unit = this@typed.beginUse()
    override fun close(code: Short, reason: String) = this@typed.close(code, reason)
    override fun onOpen(action: () -> Unit) = this@typed.onOpen(action)
    override fun onClose(action: (Short) -> Unit) = this@typed.onClose(action)
    override fun onMessage(action: (RECEIVE) -> Unit) {
        this@typed.onMessage {
            try {
                action(json.decodeFromString(receive, it))
            } catch (e: CancellationException) {
                /*squish*/
            } catch (e: Exception) {
                @OptIn(ExperimentalSerializationApi::class)
                Exception(
                    "Failed to decode message; expected a ${receive.descriptor.serialName} but got '${it.take(150)}'",
                    e
                ).report()
            }
        }
    }

    override fun send(data: SEND) {
        this@typed.send(json.encodeToString(send, data))
    }
}

@Deprecated("RetryWebSocket", ReplaceWith("RetryWebSocket")) public typealias RetryWebsocket = RetryWebSocket
public interface RetryWebSocket : WebSocket, TypedWebSocket<String, String> {
    public fun retryNow() {

    }
}


public interface TypedWebSocket<SEND, RECEIVE> : ResourceUse {
    public val connected: Reactive<Boolean>

    public fun close(code: Short, reason: String)
    public fun send(data: SEND)
    public fun onOpen(action: () -> Unit)
    public fun onMessage(action: (RECEIVE) -> Unit)
    public fun onClose(action: (Short) -> Unit)
}


public val <RECEIVE> TypedWebSocket<*, RECEIVE>.mostRecentMessage: Reactive<RECEIVE?>
    get() = object : Reactive<RECEIVE?> {
        var value: RECEIVE? = null
            private set

        val listeners = ArrayList<() -> Unit>()

        init {
            onMessage {
                value = it
                listeners.invokeAllSafe()
            }
        }

        override val state: ReactiveState<RECEIVE?> get() = ReactiveState(value)

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            val parent = this@mostRecentMessage.beginUse()
            return { listeners.remove(listener); parent() }
        }
    }