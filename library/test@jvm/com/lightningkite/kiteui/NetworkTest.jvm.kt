package com.lightningkite.kiteui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun runPlatformTest(block: suspend CoroutineScope.() -> Unit): TestResult = runBlocking {
    // WebSocketWrapper delivers every callback on Dispatchers.Main, which a plain JVM test lacks.
    Dispatchers.setMain(Dispatchers.IO)
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
