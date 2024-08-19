package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun WebSocket.waitUntilConnect(delay: suspend (Long) -> Unit = { com.lightningkite.kiteui.delay(it) }) {
    suspendCoroutineCancellable<Unit> {
        onOpen {
            launchGlobal {
                delay(1000L)
                it.resume(Unit)
            }
        }
        onClose { code ->
            it.resumeWithException(ConnectionException("Socket closed almost immediately.  Code $code"))
        }
        return@suspendCoroutineCancellable {}
    }
}

fun retryWebsocket(
    url: String,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Console? = null
): RetryWebsocket = retryWebsocket(
    underlyingSocket = { websocket(url) },
    pingTime = pingTime,
    gate = gate,
    log = log
)
fun retryWebsocket(
    underlyingSocket: () -> WebSocket,
    pingTime: Long,
    gate: ConnectivityGate = Connectivity.fetchGate,
    log: Console? = null
): RetryWebsocket {
    log?.log("Creating")
    val baseDelay = 1000L
    var currentDelay = baseDelay
    var lastConnect = 0.0
    val connected = Property(false).also {
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
    fun reset() {
        val id = instanceCount++
        currentWebSocketId = id
        currentWebSocket = underlyingSocket().also { socket ->
            var pings: Cancellable? = null
            socket.onOpen {
                log?.log("$id onOpen")
                onOpenList.toList().forEach { l -> l() }
            }
            socket.onMessage {
                log?.log("$id onMessage $it")
                lastPong = clockMillis()
                if (it.isNotBlank()) onMessageList.toList().forEach { l -> l(it) }
            }
            socket.onBinaryMessage {
                log?.log("$id onBinaryMessage $it")
                onBinaryMessageList.toList().forEach { l -> l(it) }
            }
            socket.onClose {
                log?.log("$id onClose $it")
                onCloseList.toList().forEach { l -> l(it) }
            }
            socket.onOpen {
                lastConnect = clockMillis()
                lastPong = lastConnect
                connected.value = true
                pings?.cancel()
                pings = launchGlobal {
                    while (true) {
                        delay(pingTime)
                        val now = clockMillis()
                        when {
                            lastPong < now - (pingTime * 3) -> socket.close(
                                3000,
                                "Server did not respond to three consecutive pings."
                            )

                            lastPong < now - pingTime.times(0.8) -> socket.send(" ")
                        }
                    }
                }
            }
            socket.onClose {
                pings?.cancel()
                currentDelay *= 2
                if (connected.value && clockMillis() - lastConnect > (pingTime * 2)) currentDelay = baseDelay
                connected.value = false
            }
        }
    }

    return object : RetryWebsocket, CalculationContext {

        override val connected: Readable<Boolean>
            get() = connected
        var listenerCounter = 0
        val shouldBeOn = Property(false)

        override fun start(): () -> Unit {
            if(listenerCounter++ == 0) shouldBeOn.value = true
            return {
                if(--listenerCounter == 0) shouldBeOn.value = false
            }
        }

        init {
            var starting = false
            reactiveScope {
                val shouldBeOn = shouldBeOn.await()
                val isOn = connected.await()
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
                            if (e is CancelledException) return@launch
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

        override fun notifyStart() {}
        override fun onRemove(action: () -> Unit) {
        }
    }
}

fun <SEND, RECEIVE> RetryWebsocket.typed(
    json: Json,
    send: KSerializer<SEND>,
    receive: KSerializer<RECEIVE>,
): TypedWebSocket<SEND, RECEIVE> = object : TypedWebSocket<SEND, RECEIVE> {
    override val connected: Readable<Boolean>
        get() = this@typed.connected

    override fun start(): () -> Unit = this@typed.start()
    override fun close(code: Short, reason: String) = this@typed.close(code, reason)
    override fun onOpen(action: () -> Unit) = this@typed.onOpen(action)
    override fun onClose(action: (Short) -> Unit) = this@typed.onClose(action)
    override fun onMessage(action: (RECEIVE) -> Unit) {
        this@typed.onMessage {
            try {
                action(json.decodeFromString(receive, it))
            } catch (e: Exception) {
                Exception("Failed to decode message; expected a ${receive.descriptor.serialName} but got '${it.take(150)}'", e).report()
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
    val connected: Readable<Boolean>

    fun close(code: Short, reason: String)
    fun send(data: SEND)
    fun onOpen(action: () -> Unit)
    fun onMessage(action: (RECEIVE) -> Unit)
    fun onClose(action: (Short) -> Unit)
}


val <RECEIVE> TypedWebSocket<*, RECEIVE>.mostRecentMessage: Readable<RECEIVE?>
    get() = object : Readable<RECEIVE?> {
        var value: RECEIVE? = null
            private set

        val listeners = ArrayList<() -> Unit>()

        init {
            onMessage {
                value = it
                listeners.invokeAllSafe()
            }
        }

        override val state: ReadableState<RECEIVE?> get() = ReadableState(value)

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            val parent = this@mostRecentMessage.start()
            return { listeners.remove(listener); parent() }
        }
    }