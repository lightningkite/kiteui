package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel

/**
 * [retryWebSocket] against a server that really does hang up.
 *
 * Reconnection is the whole reason this wrapper exists and it is the part no unit test can fake
 * convincingly: what it has to survive is a socket dying underneath it, which only a real one does.
 * The server labels each connection, so a test can tell "still connected" from "connected again".
 */
class RetryWebSocketTest {

    @Test
    fun reconnectsAfterTheServerHangsUp() = networkTest {
        withRetrySocket { socket, messages ->
            val first = messages.receive()
            assertTrue(first.startsWith(TestServer.sessionPrefix), "unexpected greeting: $first")

            socket.send(TestServer.dropCommand)

            val second = messages.receive()
            assertTrue(second.startsWith(TestServer.sessionPrefix), "unexpected greeting: $second")
            assertNotEquals(first, second, "the socket should have come back on a new connection")
        }
    }

    /** Between drops it is an ordinary socket, and messages have to keep flowing through it. */
    @Test
    fun carriesMessagesOnTheReconnectedSocket() = networkTest {
        withRetrySocket { socket, messages ->
            messages.receive()
            socket.send(TestServer.dropCommand)
            messages.receive()

            socket.send("after-reconnect")
            assertEquals("after-reconnect", messages.receive())
        }
    }

    /** [RetryWebSocket.connected] is what a UI binds to, so it has to report a live socket. */
    @Test
    fun reportsWhenItIsConnected() = networkTest {
        withRetrySocket { socket, messages ->
            messages.receive()
            assertEquals(true, socket.connected.state.getOrNull(), "a socket that just delivered a message is connected")
        }
    }
}

/**
 * Opens a retrying socket to the server's session endpoint, holds it open for [block], and releases
 * it afterwards.
 *
 * The socket only connects while something is using it — that is what [ResourceUse.beginUse] means
 * — so the returned handle has to be held for the whole test and released at the end, or the socket
 * stays down and every receive times out. Its own gate, rather than the process-wide one, so a
 * failure here cannot leave later tests waiting out a backoff.
 */
private suspend fun withRetrySocket(block: suspend (RetryWebSocket, Channel<String>) -> Unit) {
    val socket = retryWebSocket(
        url = "${TestServer.ws}/ws/session",
        pingTime = retryPingTime,
        gate = ConnectivityGate(),
    )
    val messages = Channel<String>(Channel.UNLIMITED)
    socket.onMessage { messages.trySend(it) }
    val release = socket.beginUse()
    try {
        block(socket, messages)
    } finally {
        release()
        socket.close(1000, "test finished")
    }
}

/** Longer than any of these tests take, so keepalive traffic never confuses what they assert on. */
private const val retryPingTime = 60_000L
