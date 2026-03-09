package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

// by Claude — Bug 9: removed artificial 100ms delay; resume immediately on connect
suspend fun WebSocket.waitUntilConnect(delay: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }) {
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

fun retryWebsocket(
    url: String,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebsocket = retryWebsocket(
    underlyingSocket = { websocket(url) },
    pingTime = pingTime,
    gate = gate,
    log = log
)

fun retryWebsocket(
    underlyingSocket: suspend () -> WebSocket,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Log? = null,
): RetryWebsocket {
    log?.log("Creating")
    var lastConnect = 0.0
    val connected = Signal(false).also {
        it.addListener {
            log?.log("connected: ${it.value}")
        }
    }
    var currentWebSocket: WebSocket? = null
    val onOpenList = ArrayList<() -> Unit>()
    val onMessageList = ArrayList<(String) -> Unit>()
    val onBinaryMessageList = ArrayList<(Blob) -> Unit>()
    val onCloseList = ArrayList<(Short) -> Unit>()
    var lastPong = clockMillis()
    var instanceCount: Int = 0
    var currentWebSocketId = -1
    suspend fun reset() {
        val id = instanceCount++
        currentWebSocketId = id
        currentWebSocket?.close(1000, "Reconnecting")
        currentWebSocket = underlyingSocket().also { socket ->
            var pings: Job? = null
            socket.onOpen {
                if (id != currentWebSocketId) return@onOpen
                log?.log("$id onOpen")
                onOpenList.toList().forEach { l -> l() }
            }
            socket.onMessage {
                if (id != currentWebSocketId) return@onMessage
                log?.log("$id onMessage $it")
                lastPong = clockMillis()
                if (it.isNotBlank()) onMessageList.toList().forEach { l -> l(it) }
            }
            socket.onBinaryMessage {
                if (id != currentWebSocketId) return@onBinaryMessage
                log?.log("$id onBinaryMessage $it")
                onBinaryMessageList.toList().forEach { l -> l(it) }
            }
            socket.onClose {
                if (id != currentWebSocketId) return@onClose
                log?.log("$id onClose $it")
                onCloseList.toList().forEach { l -> l(it) }
            }
            socket.onOpen {
                if (id != currentWebSocketId) return@onOpen
                lastConnect = clockMillis()
                lastPong = lastConnect
                connected.value = true
                pings?.cancel()
                pings = AppScope.launch {
                    while (true) {
                        delay(pingTime)
                        if (id != currentWebSocketId) return@launch
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
                if (id != currentWebSocketId) return@onClose
                pings?.cancel()
                connected.value = false
            }
        }
    }

    return object : RetryWebsocket, CalculationContext {

        override val connected: Reactive<Boolean>
            get() = connected
        var listenerCounter = 0
        val shouldBeOn = Signal(false)

        override fun beginUse(): () -> Unit {
            if (listenerCounter++ == 0) shouldBeOn.value = true
            return {
                if (--listenerCounter == 0) shouldBeOn.value = false
            }
        }

        override val coroutineContext: CoroutineContext = SupervisorJob()

        init {
            var starting = false
            reactiveScope {
                val shouldBeOn = shouldBeOn()
                val isOn = connected()
                if (shouldBeOn && !isOn && !starting) {
                    starting = true
                    launch {
                        try {
                            gate.run("WS") {
                                log?.log("starting")
                                reset()
                                currentWebSocket?.waitUntilConnect(gate.delay)
                                log?.log("started A")
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) return@launch
                            log?.log("start fail: $e")
                            e.printStackTrace2()
                        } finally {
                            starting = false
                        }
                    }
                } else if (!shouldBeOn && isOn) {
                    currentWebSocket?.close(1000, "OK")
                }
            }
        }

        override fun close(code: Short, reason: String) {
            log?.log("close $code")
            currentWebSocket?.close(code, reason)
            currentWebSocket = null
            currentWebSocketId = -1
        }

        override fun send(data: Blob) {
            log?.log("$currentWebSocketId send $data")
            currentWebSocket?.send(data)
        }

        override fun send(data: String) {
            log?.log("$currentWebSocketId send $data")
            currentWebSocket?.send(data)
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

fun <SEND, RECEIVE> RetryWebsocket.typed(
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

interface RetryWebsocket : WebSocket, TypedWebSocket<String, String> {
    fun retryNow() {

    }
}


interface TypedWebSocket<SEND, RECEIVE> : ResourceUse {
    val connected: Reactive<Boolean>

    fun close(code: Short, reason: String)
    fun send(data: SEND)
    fun onOpen(action: () -> Unit)
    fun onMessage(action: (RECEIVE) -> Unit)
    fun onClose(action: (Short) -> Unit)
}


val <RECEIVE> TypedWebSocket<*, RECEIVE>.mostRecentMessage: Reactive<RECEIVE?>
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