package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.test.TestResult

// The browser's event loop is already running and its callbacks already arrive on it, so the only
// thing to arrange is handing the framework a promise to await. The cast mirrors what
// kotlinx-coroutines-test does internally: TestResult is a non-generic declaration of Promise<Unit>,
// which a generic Promise cannot be assigned to without it.
@OptIn(DelicateCoroutinesApi::class)
@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun runPlatformTest(block: suspend CoroutineScope.() -> Unit): TestResult =
    GlobalScope.promise { block() } as TestResult
