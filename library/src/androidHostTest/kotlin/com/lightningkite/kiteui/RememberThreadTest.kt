package com.lightningkite.kiteui

import android.os.Looper
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * Reactive state and the bindings listening to it are main-thread only, so a calculation that suspends must carry
 * on, and notify its listeners, on the main thread, whichever thread resumed it.
 */
@RunWith(RobolectricTestRunner::class)
class RememberThreadTest {
    private val main: Thread = Looper.getMainLooper().thread

    // Robolectric's main looper only runs posted work when idled, and this test body is its thread.  Its clock only
    // moves when told to, so delays on the main dispatcher (Handler.postDelayed) need idleFor.
    private fun <T : Any> pumpMainUntil(get: () -> T?): T {
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.MILLISECONDS)
            get()?.let { return it }
            Thread.sleep(5)
        }
        error("Timed out")
    }

    private fun assertCalculatesAndNotifiesOnMain(observed: Reactive<Thread>) {
        var notifiedOn: Thread? = null
        val release = observed.addListener { notifiedOn = Thread.currentThread() }
        val calculatedOn = pumpMainUntil { observed.state.takeIf { it.success }?.getOrNull() }
        release()
        assertEquals(main, calculatedOn)
        assertEquals(main, notifiedOn)
    }

    @Test
    fun rememberSuspendingAfterDelay() = assertCalculatesAndNotifiesOnMain(rememberSuspending {
        delay(20)
        Thread.currentThread()
    })

    @Test
    fun rememberSuspendingAfterWithContextDefault() = assertCalculatesAndNotifiesOnMain(rememberSuspending {
        withContext(Dispatchers.Default) { Thread.sleep(20) }
        Thread.currentThread()
    })

    @Test
    fun rememberAfterAsyncDelay() = assertCalculatesAndNotifiesOnMain(remember {
        async("wait") { delay(20) }
        Thread.currentThread()
    })
}
