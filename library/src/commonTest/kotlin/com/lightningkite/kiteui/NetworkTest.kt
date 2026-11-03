package com.lightningkite.kiteui

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withTimeout

/**
 * Runs [block] against the platform's real networking stack, on every target the library builds for.
 *
 * These tests are worth writing in common precisely because [platformFetch] and [platformWebSocket]
 * are the least shared code in the library — four independent implementations over OkHttp,
 * NSURLSession, the browser, and Android's stack — and the contract they are meant to meet is
 * identical. A common test is the only place that claim can be checked.
 *
 * Deliberately not [kotlinx.coroutines.test.runTest]: its scheduler advances virtual time, so a
 * [withTimeout] around a real socket expires before the first packet moves. Here time is real and
 * the platform's own event loop is running, which is also what the socket implementations need —
 * they deliver callbacks on the main dispatcher, and on iOS that means a pumped run loop.
 */
internal fun networkTest(block: suspend CoroutineScope.() -> Unit): TestResult =
    runPlatformTest { withTimeout(networkTestTimeout) { block() } }

/**
 * Long enough for a loopback round trip on a loaded CI machine, short enough that a socket which
 * never answers fails the test rather than hanging the build.
 */
private val networkTestTimeout = 30.seconds

/**
 * Runs [block] to completion on real time, with whatever the platform's networking needs in order to
 * deliver its callbacks:
 *
 * - JVM and Android install a main dispatcher, which a plain test does not otherwise have, and
 *   Android also needs a context before its HTTP client can be built.
 * - iOS pumps the main run loop, since its callbacks are posted to the main queue and nothing else
 *   in a native test drains it.
 * - JS hands back the promise the test framework already knows how to await.
 */
internal expect fun runPlatformTest(block: suspend CoroutineScope.() -> Unit): TestResult
