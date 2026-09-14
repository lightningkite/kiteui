package com.lightningkite.kiteui

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

/**
 * What [platformWebSocket] must do identically on every platform, checked against a real server.
 *
 * As with [HttpFetchTest], the value is in the agreement: four implementations — Ktor over OkHttp,
 * Ktor over NSURLSession, the browser's own `WebSocket`, and Android's client — are supposed to
 * present one interface, and only a common test says whether they do.
 */
class WebSocketTest {

    @Test
    fun echoesText() = networkTest {
        withSocket("${TestServer.ws}/ws/echo") { socket ->
            val payload = "hello-kiteui-${Random.nextInt()}"
            socket.send(payload)
            assertEquals(payload, socket.text.receive())
        }
    }

    /** Order is part of the contract: a socket that reorders messages breaks any protocol over it. */
    @Test
    fun preservesMessageOrder() = networkTest {
        withSocket("${TestServer.ws}/ws/echo") { socket ->
            val sent = (1..20).map { "message-$it" }
            sent.forEach { socket.send(it) }
            assertEquals(sent, sent.map { socket.text.receive() })
        }
    }

    @Test
    fun echoesBinary() = networkTest {
        withSocket("${TestServer.ws}/ws/echo") { socket ->
            val payload = TestServer.deterministicBytes(1024)
            socket.send(payload.toBlob("application/octet-stream"))
            assertEquals(payload.toList(), socket.binary.receive().toByteArray().toList())
        }
    }

    /**
     * The close code is how a server tells a client why it went away, and everything above this
     * layer — [retryWebSocket] deciding whether to reconnect, most of all — reads it.
     */
    @Test
    fun reportsTheServersCloseCode() = networkTest {
        withSocket("${TestServer.ws}/ws/close?code=$applicationCloseCode&reason=bye", waitForOpen = false) { socket ->
            assertEquals(applicationCloseCode, socket.closed.await())
        }
    }

    /**
     * A socket that never opens must still report a close, or a caller waiting on
     * [waitUntilConnect] waits forever. Nothing is listening on this address.
     */
    @Test
    fun reportsCloseWhenTheConnectionIsRefused() = networkTest {
        withSocket(TestServer.refusedWs, waitForOpen = false) { socket ->
            socket.closed.await()
        }
    }

    /** Sockets must not share state; two at once is ordinary in an app with several live queries. */
    @Test
    fun keepsConcurrentSocketsIndependent() = networkTest {
        withSocket("${TestServer.ws}/ws/echo") { first ->
            withSocket("${TestServer.ws}/ws/echo") { second ->
                first.send("to-first")
                second.send("to-second")
                assertEquals("to-first", first.text.receive())
                assertEquals("to-second", second.text.receive())
            }
        }
    }

    @Test
    fun closesOnRequest() = networkTest {
        val socket = RecordedSocket(webSocket("${TestServer.ws}/ws/echo"))
        socket.waitUntilConnect()
        socket.close(1000, "done")
        assertTrue(socket.closed.await() >= 1000, "a closed socket should report a close code")
    }

    /** In the 4000-4999 range applications may choose from; the reserved codes are the server's. */
    private val applicationCloseCode: Short = 4321
}

/**
 * A socket with everything it reported kept for the test to await.
 *
 * Callbacks arrive on the platform's main dispatcher while the test body runs elsewhere, so the
 * events have to be recorded as they happen rather than awaited where they occur. Unbounded channels
 * because dropping a message would turn a real ordering bug into a mysterious timeout.
 */
internal class RecordedSocket(private val socket: WebSocket) : WebSocket by socket {
    val text: Channel<String> = Channel(Channel.UNLIMITED)
    val binary: Channel<Blob> = Channel(Channel.UNLIMITED)
    val closed: CompletableDeferred<Short> = CompletableDeferred()

    init {
        socket.onMessage { text.trySend(it) }
        socket.onBinaryMessage { binary.trySend(it) }
        socket.onClose { closed.complete(it) }
    }
}

/**
 * Opens [url], runs [block] against it, and closes it however [block] ends.
 *
 * [waitForOpen] is false for the tests about sockets that are never meant to open.
 */
internal suspend fun withSocket(
    url: String,
    waitForOpen: Boolean = true,
    block: suspend (RecordedSocket) -> Unit,
) {
    val socket = RecordedSocket(webSocket(url))
    try {
        if (waitForOpen) socket.waitUntilConnect()
        block(socket)
    } finally {
        socket.close(1000, "test finished")
    }
}
