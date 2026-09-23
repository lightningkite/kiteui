package com.lightningkite.kiteui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.test.AfterTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


/**
 * [WaitGate] against a waiter that comes straight back for more.
 *
 * Whoever a gate resumes may continue on the resumer's own stack - [kotlinx.coroutines.Dispatchers.Main.immediate]
 * reports no dispatch needed, and the socket retry loop resumed by [ConnectivityGate.abandon] goes
 * straight back to awaiting - so the gate has to be able to hand back a waiter that immediately
 * re-registers. Resuming while iterating the list it re-registers into feeds the loop its own new
 * entry, endlessly and without ever suspending, which on a browser is a frozen page rather than a
 * failed request.
 *
 * Both tests are bounded so a regression fails the build instead of hanging it.
 */
class WaitGateReentryTest {

    /** Re-awaits on cancellation exactly as the socket retry loop does. */
    private fun WaitGate.waiterThatComesBack(limit: Int, count: () -> Unit): Job =
        CoroutineScope(Dispatchers.Unconfined).launch {
            repeat(limit) {
                count()
                try {
                    await()
                } catch (e: CancellationException) {
                    // Deliberately swallowed: abandon() delivers one of these to a healthy waiter.
                }
            }
        }

    @Test
    fun abandoningAWaiterThatComesBackResumesItOnce() {
        val gate = WaitGate()
        var registrations = 0
        val job = gate.waiterThatComesBack(50) { registrations++ }

        gate.abandon()

        assertEquals(2, registrations, "the waiter should have registered once more, not been fed its own re-registration")
        job.cancel()
    }

    @Test
    fun openingTheGateForAWaiterThatFailsAndComesBackResumesItOnce() {
        val gate = WaitGate()
        var registrations = 0
        // The shape ConnectivityGate.run has: wait, attempt, and on failure close the gate again
        // before waiting once more.  The close happens inside the waiter, so it lands while the
        // resumer is still working through its list.
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            repeat(50) {
                registrations++
                gate.await()
                gate.permit = false
            }
        }

        gate.permit = true

        assertEquals(2, registrations, "the waiter should have registered once more, not been fed its own re-registration")
        job.cancel()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityGateTest {
    /**
     * The distinction the gate exists to draw: a blocked request will be refused identically on every
     * attempt, so retrying it can only spin forever.
     */
    @Test fun blockedRequestsAreNotRetried() = runTest {
        var attempts = 0
        assertFailsWith<RequestBlockedException> {
            ConnectivityGate(delay = {}).run("test") {
                attempts++
                throw RequestBlockedException("CORS")
            }
        }
        assertEquals(1, attempts)
    }

    /** Callers wanting either failure catch the base type; catching [ConnectionException] gets only retryable ones. */
    @Test fun bothFailuresShareABaseType() {
        val caught = mutableListOf<String>()
        for (e in listOf(ConnectionException("offline"), RequestBlockedException("CORS"))) {
            try {
                throw e
            } catch (f: FetchException) {
                caught.add(f::class.simpleName!!)
            }
        }
        assertEquals(listOf("ConnectionException", "RequestBlockedException"), caught)
    }

    /** The status-code failures refine [ConnectionException], so existing catches keep working. */
    @Test fun statusFailuresAreStillRetryable() {
        val caught = mutableListOf<String>()
        for (e in listOf(ServerUnavailableException("503"), RateLimitedException("429"))) {
            try {
                throw e
            } catch (f: ConnectionException) {
                caught.add(f::class.simpleName!!)
            }
        }
        assertEquals(listOf("ServerUnavailableException", "RateLimitedException"), caught)
    }

    /**
     * Drives one failure through a gate and reports the wait it scheduled.
     *
     * Reads the wait from [ConnectivityGate.retryAt], which [ConnectivityGate.run] sets on the
     * caller's own coroutine, rather than from the injected delay, which the timer reaches on a
     * different dispatcher and could not be observed without a race.
     */
    private suspend fun TestScope.scheduleOnce(gate: ConnectivityGate, failure: ConnectionException): Duration {
        val job = launch { gate.run("test") { throw failure } }
        runCurrent()
        val wait = gate.retryAt.value!! - FIXED_NOW
        job.cancel()
        return wait
    }

    /**
     * A gate whose retry timer parks forever, so the schedule it chose can be read without the timer
     * racing the assertions to reopen the gate.
     *
     * The timer runs in [timerScope] rather than the test scope because `runTest` would wait on a
     * coroutine that never finishes, and not in [AppScope] because that needs a main dispatcher this
     * target does not have.
     */
    private fun gate(seed: Int) = ConnectivityGate(
        clock = object : Clock { override fun now(): Instant = FIXED_NOW },
        delay = { awaitCancellation() },
        random = Random(seed),
        scope = { timerScope },
    )

    private val timerScope = CoroutineScope(Job())

    @AfterTest fun stopTimers() { timerScope.cancel() }

    /**
     * The property jitter exists for: identical gates failing at the same instant must not choose the
     * same moment to come back, or a recovering server receives the whole fleet at once.
     */
    @Test fun fullJitterSpreadsRetriesAcrossTheCeiling() = runTest {
        val waits = (1..24).map { seed -> scheduleOnce(gate(seed), ServerUnavailableException("503")) }
        for (wait in waits) {
            assertTrue(wait >= Duration.ZERO, "negative wait $wait")
            assertTrue(wait <= 10.seconds, "wait $wait exceeded the first ceiling")
        }
        assertTrue(waits.distinct().size > 1, "every gate picked the same wait: $waits")
    }

    /** The ceiling is what escalates; jitter only scatters arrivals underneath it. */
    @Test fun ceilingDoublesPerFailureAndCapsAtMax() = runTest {
        val gate = gate(seed = 7)
        val ceilings = mutableListOf<Duration>()
        repeat(8) {
            scheduleOnce(gate, ServerUnavailableException("503"))
            ceilings.add(gate.nextRetry)
            // Stand in for the timer firing, so the next failure schedules afresh.
            gate.retryAt.value = null
            gate.gate.permit = true
        }
        assertEquals(
            // 160 doubles to 320, which the 5-minute cap clamps to 300.
            listOf(20.seconds, 40.seconds, 80.seconds, 160.seconds, 300.seconds, 300.seconds, 300.seconds, 300.seconds),
            ceilings,
        )
    }

    /**
     * The reset is the point, not the release. Retrying on an escalated interval is what drove apps
     * to call [ConnectivityGate.retryNow] twice in a row hoping one attempt landed.
     */
    @Test fun retryNowReturnsScheduleToBase() = runTest {
        val gate = gate(seed = 3)
        repeat(3) {
            scheduleOnce(gate, ServerUnavailableException("503"))
            gate.retryAt.value = null
            gate.gate.permit = true
        }
        assertTrue(gate.nextRetry > gate.baseRetry, "expected an escalated ceiling, got ${gate.nextRetry}")

        gate.retryNow()

        assertEquals(gate.baseRetry, gate.nextRetry)
        assertEquals(null, gate.retryAt.value)
        assertTrue(gate.gate.permit)
    }

    /** A success means the trouble has passed; the next failure starts from base. */
    @Test fun successReturnsScheduleToBase() = runTest {
        val gate = gate(seed = 5)
        scheduleOnce(gate, ServerUnavailableException("503"))
        gate.retryAt.value = null
        gate.gate.permit = true

        assertEquals("ok", gate.run("test") { "ok" })

        assertEquals(gate.baseRetry, gate.nextRetry)
    }

    /**
     * `Retry-After` is an instruction, so the wait never falls below it — but every throttled client
     * was handed the same deadline, so it is spread rather than obeyed to the second.
     */
    @Test fun retryAfterIsHonoredThenSpread() = runTest {
        val asked = 120.seconds
        val waits = (1..16).map { seed ->
            scheduleOnce(gate(seed), RateLimitedException("429", retryAfter = asked))
        }
        for (wait in waits) {
            assertTrue(wait >= asked, "$wait came earlier than the server's $asked")
            assertTrue(wait <= asked + 10.seconds, "$wait overshot the spreading window")
        }
        assertTrue(waits.distinct().size > 1, "every client picked the same moment: $waits")
    }

    /**
     * What the retry scope exists to guarantee: an attempt re-runs the *whole* operation, so anything
     * derived per attempt is derived again. An access token is the case that matters — one can easily
     * expire while the gate waits out a five-minute backoff, and replaying it would have the retry
     * fail on authentication instead of succeeding.
     *
     * This is the regression that a decorator-shaped retry layer invites: with retry as one more
     * [HttpFetcher] in a chain, putting it outside or inside the header layer typechecks identically
     * and only one of the two recomputes.
     */
    @Test fun everyAttemptRecomputesTheWholeOperation() = runTest {
        val gate = ConnectivityGate(
            clock = object : Clock { override fun now(): Instant = FIXED_NOW },
            delay = {}, // reopen as soon as the timer is armed, so the test does not wait
            random = Random(2),
            scope = { timerScope },
        )
        val tokensIssued = mutableListOf<String>()
        var attempts = 0

        val tokenThatWorked = gate.run("test") {
            val token = "token-${tokensIssued.size}"
            tokensIssued.add(token)
            if (++attempts < 3) throw ConnectionException("still down")
            token
        }

        assertEquals(3, attempts)
        assertEquals(listOf("token-0", "token-1", "token-2"), tokensIssued)
        assertEquals("token-2", tokenThatWorked, "the successful attempt reused an earlier attempt's token")
    }

    /** Without a header there is nothing to honor, so it falls back to the computed ceiling. */
    @Test fun rateLimitWithoutHeaderUsesComputedBackoff() = runTest {
        val wait = scheduleOnce(gate(seed = 11), RateLimitedException("429", retryAfter = null))
        assertTrue(wait <= 10.seconds, "expected the first ceiling, got $wait")
    }
}

private val FIXED_NOW = Instant.fromEpochMilliseconds(0)
