package com.lightningkite.kiteui

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

/**
 * [retryWebSocket]'s keepalive runs on main, so a socket nobody uses any more must stop waking it.
 *
 * Main is replaced by a dispatcher that counts what is dispatched to it; after the socket is closed
 * or released, that count must stand still.  Real time rather than virtual, because the ping loop
 * judges silence by [clockMillis].
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
class RetryWebSocketPingTest {

    private val mainWork = AtomicInt(0)
    private val countingMain = object : CoroutineDispatcher() {
        val inner = Dispatchers.Default.limitedParallelism(1)
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            mainWork.incrementAndFetch()
            inner.dispatch(context, block)
        }
    }

    @BeforeTest fun replaceMain() { Dispatchers.setMain(countingMain) }
    @AfterTest fun restoreMain() { Dispatchers.resetMain() }

    /**
     * Opens only when told, answers every ping as a live server does, and never reports a close, like
     * a socket stuck in its closing handshake.
     */
    private class FakeSocket : WebSocket {
        val pings = AtomicInt(0)
        @Volatile var closed = false
        val openListeners = Channel<() -> Unit>(Channel.UNLIMITED)
        private val messages = ArrayList<(String) -> Unit>()
        override fun close(code: Short, reason: String) { closed = true }
        override fun send(data: String) {
            if (data == " ") pings.incrementAndFetch()
            messages.toList().forEach { it(" ") }
        }
        override fun send(data: Blob) {}
        override fun onOpen(action: () -> Unit) { openListeners.trySend(action) }
        override fun onMessage(action: (String) -> Unit) { messages.add(action) }
        override fun onBinaryMessage(action: (Blob) -> Unit) {}
        override fun onClose(action: (Short) -> Unit) {}
    }

    private val pingTime = 50L
    private val sockets = Channel<FakeSocket>(Channel.UNLIMITED)
    private fun retrying() = retryWebSocket(
        underlyingSocket = { FakeSocket().also { sockets.send(it) } },
        pingTime = pingTime,
        gate = ConnectivityGate(),
    )

    private suspend fun realDelay(ms: Long) = withContext(Dispatchers.Default) { delay(ms) }

    /** Opens the next socket [retryWebSocket] dials, and returns just after it has been pinged. */
    private suspend fun openAndAwaitPing(socket: RetryWebSocket): FakeSocket = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            val fake = sockets.receive()
            // Two listen for the open: retryWebSocket's own, and the attempt waiting for it to connect.
            val listeners = listOf(fake.openListeners.receive(), fake.openListeners.receive())
            withContext(Dispatchers.Main) { listeners.forEach { it() } }
            assertEquals(true, socket.connected.state.getOrNull())
            while (fake.pings.load() < 2) delay(1)
            fake
        }
    }

    /**
     * Called just after a ping, so a loop that survived would wake well inside the window.  The short
     * settle lets the cancelled loop's own completion through first.
     */
    private suspend fun assertMainStaysQuiet() {
        realDelay(pingTime / 5)
        val before = mainWork.load()
        realDelay(pingTime * 5)
        assertEquals(before, mainWork.load(), "work was still being scheduled on main")
    }

    @Test fun releasingTheLastUseStopsThePings() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        val fake = openAndAwaitPing(socket)

        release()
        // Releasing is acted on off main, after release() returns.
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!fake.closed) delay(1) } }
        assertMainStaysQuiet()
    }

    @Test fun closingStopsThePings() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        openAndAwaitPing(socket)

        release()
        socket.close(1000, "done")
        assertMainStaysQuiet()
    }

    /** Still in use, a close only drops this connection: the next one is dialed and pinged like the first. */
    @Test fun closingWhileInUseRedialsAndPingsTheNewSocket() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        val first = openAndAwaitPing(socket)

        socket.close(1000, "drop")
        val pingsOnFirst = first.pings.load()
        openAndAwaitPing(socket)
        assertEquals(pingsOnFirst, first.pings.load(), "the dropped socket was still being pinged")

        release()
        socket.close(1000, "done")
        assertMainStaysQuiet()
    }
}
