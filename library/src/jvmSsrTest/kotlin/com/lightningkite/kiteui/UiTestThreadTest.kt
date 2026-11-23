package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

/** The element tree isn't thread-safe, so a test must build, change and read it on one thread, as an app does. */
class UiTestThreadTest {
    @Test
    fun uiWorkAndTheTestBodyRunOnOneThread() {
        val threads = ConcurrentHashMap.newKeySet<Thread>()
        fun record() { threads += Thread.currentThread() }
        val ticked = Signal(false)
        uiTest(content = {
            record()
            text {
                ::content {
                    record()
                    // Resumes on an IO thread, as a response from the server does.
                    async("load") { withContext(Dispatchers.IO) { Thread.sleep(50) } }
                    "loaded"
                }
            }
            text { ::content { record(); if (ticked()) "ticked" else "waiting" } }
            // Resumes after a timer, as app-wide work does.
            AppScope.launch { delay(50); ticked.value = true }
        }) {
            record()
            waitForText("loaded")
            waitForText("ticked")
            record()
        }
        assertEquals(1, threads.size, "UI work ran on ${threads.map { it.name }}")
    }

    @Test
    fun anExceptionEscapingOntoTheUiThreadFailsTheTest() {
        val failure = assertFails {
            uiTest {
                // How a reactive calculation is queued onto the main thread from elsewhere: a bare task, not a coroutine.
                Dispatchers.Main.dispatch(EmptyCoroutineContext, Runnable { throw IllegalStateException("Crashed") })
                delay(50)
            }
        }
        assertEquals("Crashed", failure.message)
    }
}
