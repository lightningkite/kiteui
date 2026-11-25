package com.lightningkite.kiteui

import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * For Ktor's OkHttp engine (`webSocketFactory`): OkHttp sockets on which a close before the handshake
 * finishes aborts the handshake.
 *
 * Ktor answers a cancelled connect by calling `close()`, which OkHttp only queues behind the handshake,
 * so the attempt carries on until it succeeds or times out. Before open, `cancel()` is what closing means.
 */
internal class CloseAbortsHandshakeWebSocketFactory(private val client: OkHttpClient) : WebSocket.Factory {
    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        val opened = AtomicBoolean(false)
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                opened.set(true)
                listener.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) = listener.onMessage(webSocket, text)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = listener.onMessage(webSocket, bytes)
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = listener.onClosing(webSocket, code, reason)
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = listener.onClosed(webSocket, code, reason)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = listener.onFailure(webSocket, t, response)
        })
        return object : WebSocket by socket {
            override fun close(code: Int, reason: String?): Boolean {
                if (opened.get()) return socket.close(code, reason)
                socket.cancel()
                return false
            }
        }
    }
}
