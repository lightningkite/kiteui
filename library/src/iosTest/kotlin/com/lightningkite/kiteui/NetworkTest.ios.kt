package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import platform.CoreFoundation.CFRunLoopRunInMode
import platform.CoreFoundation.kCFRunLoopDefaultMode

/**
 * iOS posts every socket callback to the main queue, which runs only while the main run loop is
 * being pumped. A native test does not pump it, so the body runs as a coroutine on the main
 * dispatcher — the same place an app's callbacks arrive — while this thread spins the loop that
 * delivers them. Spinning is not a suspension point, so the test thread stays put until the body
 * finishes, which is what a test needs.
 */
@OptIn(DelicateCoroutinesApi::class)
internal actual fun runPlatformTest(block: suspend CoroutineScope.() -> Unit): TestResult {
    var outcome: Result<Unit>? = null
    GlobalScope.launch(Dispatchers.Main) { outcome = runCatching { block() } }
    while (outcome == null) CFRunLoopRunInMode(kCFRunLoopDefaultMode, runLoopSliceSeconds, false)
    outcome!!.getOrThrow()
}

/** How long each pump may block for. Short enough that the loop notices the body finishing promptly. */
private const val runLoopSliceSeconds = 0.05
