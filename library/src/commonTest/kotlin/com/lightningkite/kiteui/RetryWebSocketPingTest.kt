package com.lightningkite.kiteui

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * [retryWebSocket]'s keepalive runs on main, so a socket nobody uses any more must stop waking it.
 *
 * Main is replaced by a dispatcher that remembers every coroutine dispatched to it; once the socket is
 * closed or released, none of them may still be alive.  Asking the coroutines rather than counting
 * dispatches keeps stray timers from other tests out of it.  Real time rather than virtual, because
 * the ping loop judges silence by [clockMillis].
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
class RetryWebSocketPingTest {

    private val mainJobs = AtomicReference(setOf<Job>())
    private val recordingMain = object : CoroutineDispatcher() {
        val inner = Dispatchers.Default.limitedParallelism(1)
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            context[Job]?.let { job -> mainJobs.update { it + job } }
            inner.dispatch(context, block)
        }
    }
    private fun aliveOnMain() = mainJobs.load().filter { it.isActive }

    @BeforeTest fun replaceMain() { Dispatchers.setMain(recordingMain) }
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

    /** Opens the next socket [retryWebSocket] dials, and returns once it has been pinged. */
    private suspend fun openAndAwaitPings(socket: RetryWebSocket): FakeSocket = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            val fake = sockets.receive()
            // Two listen for the open: retryWebSocket's own, and the attempt waiting for it to connect.
            val listeners = listOf(fake.openListeners.receive(), fake.openListeners.receive())
            withContext(Dispatchers.Main) { listeners.forEach { it() } }
            assertEquals(true, socket.connected.state.getOrNull())
            while (fake.pings.load() < 2) delay(1)
            assertTrue(aliveOnMain().isNotEmpty(), "the ping loop should be running on main")
            fake
        }
    }

    @Test fun releasingTheLastUseStopsThePings() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        val fake = openAndAwaitPings(socket)

        release()
        // Releasing is acted on off main, after release() returns.
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!fake.closed) delay(1) } }
        assertEquals(listOf(), aliveOnMain(), "still running on main after the last use was released")
    }

    @Test fun closingStopsThePings() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        openAndAwaitPings(socket)

        release()
        socket.close(1000, "done")
        assertEquals(listOf(), aliveOnMain(), "still running on main after close")
    }

    /** Still in use, a close only drops this connection: the next one is dialed and pinged like the first. */
    @Test fun closingWhileInUseRedialsAndPingsTheNewSocket() = runTest {
        val socket = retrying()
        val release = socket.beginUse()
        val first = openAndAwaitPings(socket)

        socket.close(1000, "drop")
        assertEquals(listOf(), aliveOnMain(), "the dropped socket's ping loop is still running")
        val pingsOnFirst = first.pings.load()
        openAndAwaitPings(socket)
        assertEquals(pingsOnFirst, first.pings.load(), "the dropped socket was pinged")

        release()
        socket.close(1000, "done")
        assertEquals(listOf(), aliveOnMain(), "still running on main after close")
    }
}
