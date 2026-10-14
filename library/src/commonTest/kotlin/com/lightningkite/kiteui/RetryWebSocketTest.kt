package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

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

/**
 * The ways a connection attempt can end other than connected, which are the ones that decide whether
 * a socket is reliable: what matters is not that the first attempt works, but that a failed one is
 * followed by another.
 */
class RetryWebSocketRecoveryTest {

    /**
     * A failure to even build the socket - headers that could not be calculated, a token refresh
     * that threw - is still just a failure to connect, and has to be retried like any other.
     *
     * What makes it the interesting case is that it arrives as something other than a
     * [ConnectionException]. Reported as itself it escapes [ConnectivityGate.run] and ends the
     * retrying for good; reported as the failure to connect that it is, the gate keeps going.
     */
    @Test
    fun retriesAfterAnAttemptThatNeverReachedTheNetwork() = networkTest {
        var attempts = 0
        val socket = retryWebSocket(
            underlyingSocket = {
                attempts++
                if (attempts == 1) throw IllegalStateException("could not work out the headers")
                webSocket("${TestServer.ws}/ws/session")
            },
            pingTime = retryPingTime,
            gate = ConnectivityGate(random = noJitter),
        )
        val messages = Channel<String>(Channel.UNLIMITED)
        socket.onMessage { messages.trySend(it) }
        val release = socket.beginUse()
        try {
            val greeting = messages.receive()
            assertTrue(greeting.startsWith(TestServer.sessionPrefix), "unexpected greeting: $greeting")
            assertTrue(attempts >= 2, "the first attempt failed, so there had to be another")
        } finally {
            release()
            socket.close(1000, "test finished")
        }
    }

    /**
     * Taking a use and dropping it again before the attempt has had a turn must not wedge the socket.
     *
     * The attempt is launched into the retry loop's reactive context, whose coroutines are cancelled
     * before every rerun. A coroutine cancelled before its body starts never runs a `finally`, so any
     * bookkeeping cleared in one is left set - and a socket that believes an attempt is still in
     * flight never starts another, for the life of the app. A screen that mounts and unmounts in one
     * turn is all it takes.
     */
    @Test
    fun aUseDroppedBeforeTheAttemptStartsDoesNotWedgeIt() = networkTest {
        val socket = retryWebSocket(
            url = "${TestServer.ws}/ws/session",
            pingTime = retryPingTime,
            gate = ConnectivityGate(random = noJitter),
        )
        val messages = Channel<String>(Channel.UNLIMITED)
        socket.onMessage { messages.trySend(it) }
        // Taken and dropped within one turn, before anything dispatched can have run.
        socket.beginUse()()
        val release = socket.beginUse()
        try {
            val greeting = messages.receive()
            assertTrue(greeting.startsWith(TestServer.sessionPrefix), "unexpected greeting: $greeting")
        } finally {
            release()
            socket.close(1000, "test finished")
        }
    }

    /**
     * [RetryWebSocket.close] drops the connection; releasing the use is what stops the socket. So a
     * close while something still holds a use has to come back on a new connection.
     *
     * The failure this guards against is silent and permanent: the wrapper disowns the socket it
     * just closed, which also discards the closure the socket reports, so it goes on believing it is
     * connected - and a socket that believes it is connected never redials and sends into nothing.
     */
    @Test
    fun closingWhileStillInUseComesBackOnANewConnection() = networkTest {
        withRetrySocket { socket, messages ->
            val first = messages.receive()

            socket.close(1000, "dropping this connection")
            assertEquals(false, socket.connected.state.getOrNull(), "a closed socket is not connected")

            val second = messages.receive()
            assertNotEquals(first, second, "close() while still in use should have redialed")
        }
    }

    /** Closing has to reach the listeners too, or nothing downstream knows to resubscribe. */
    @Test
    fun closingTellsTheListenersItClosed() = networkTest {
        withRetrySocket { socket, messages ->
            messages.receive()
            val closes = Channel<Short>(Channel.UNLIMITED)
            socket.onClose { closes.trySend(it) }

            socket.close(4001, "dropping this connection")

            assertEquals(4001.toShort(), closes.receive(), "the close code should have reached the listener")
        }
    }
}

/**
 * A gate that schedules every retry for right now.
 *
 * [ConnectivityGate] waits a random point below its ceiling, which is right in production and makes
 * a test that has to see a second attempt wait out an arbitrary slice of ten seconds. Zeroing the
 * randomness leaves the schedule itself untouched.
 */
private val noJitter = object : kotlin.random.Random() {
    override fun nextBits(bitCount: Int): Int = 0
}

