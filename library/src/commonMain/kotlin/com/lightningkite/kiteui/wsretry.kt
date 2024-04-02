package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

fun retryWebsocket(
    url: String,
): RetryWebsocket {
    val baseDelay = 1000L
    var currentDelay = baseDelay
    var lastConnect = 0.0
    val connected = Property(false)
    var currentWebSocket: WebSocket? = null
    val onOpenList = ArrayList<() -> Unit>()
    val onMessageList = ArrayList<(String) -> Unit>()
    val onBinaryMessageList = ArrayList<(Blob) -> Unit>()
    val onCloseList = ArrayList<(Short) -> Unit>()
    var lastPong = clockMillis()
    fun reset() {
        currentWebSocket = websocket(url).also {
            it.onOpen {
                onOpenList.toList().forEach { l -> l() }
            }
            it.onMessage {
                if (it.isNotBlank()) onMessageList.toList().forEach { l -> l(it) }
                else lastPong = clockMillis()
            }
            it.onBinaryMessage {
                onBinaryMessageList.toList().forEach { l -> l(it) }
            }
            it.onClose {
                onCloseList.toList().forEach { l -> l(it) }
            }
            it.onOpen {
                lastConnect = clockMillis()
                connected.value = true
            }
            it.onClose {
                currentDelay *= 2
                if (connected.value && clockMillis() - lastConnect > 60_000.0) currentDelay = baseDelay
                connected.value = false
            }

            val pings = launchGlobal {
                while (true) {
                    delay(30_000)
                    if (lastPong < clockMillis() - 60_000) it.close(
                        3000,
                        "Server did not respond to two consecutive pings."
                    )
                    it.send(" ")
                }
            }

            onCloseList.add {
                pings.cancel()
            }

        }
    }

    return object : RetryWebsocket, CalculationContext {

        override val connected: Readable<Boolean>
            get() = connected
        val shouldBeOn = Property(0)

        override fun start(): () -> Unit {
            shouldBeOn.value++
            return {
                shouldBeOn.value--
            }
        }

        init {
            reactiveScope {
                val shouldBeOn = shouldBeOn.await() > 0
                val isOn = connected.await()
                delay(currentDelay)
                if (shouldBeOn && !isOn) {
                    reset()
                } else if (!shouldBeOn && isOn) {
                    currentWebSocket?.close(1000, "OK")
                }
            }
        }

        override fun close(code: Short, reason: String) {
            currentWebSocket?.close(code, reason)
            currentWebSocket = null
        }

        override fun send(data: Blob) {
            currentWebSocket?.send(data)
        }

        override fun send(data: String) {
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

interface RetryWebsocket : WebSocket, TypedWebSocket<String, String> {
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
                listeners.forEach { it() }
            }
        }

        override val state: ReadableState<RECEIVE?> get() = ReadableState(value)

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            val parent = this@mostRecentMessage.start()
            return { listeners.remove(listener); parent() }
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
                e.printStackTrace2()
            }
        }
    }

    override fun send(data: SEND) {
        this@typed.send(json.encodeToString(send, data))
    }
}