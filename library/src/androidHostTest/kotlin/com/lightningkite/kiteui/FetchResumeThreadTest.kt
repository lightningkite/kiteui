package com.lightningkite.kiteui

import android.os.Looper
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.rememberSuspending
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals

/**
 * Real OkHttp requests to the test server, checking where the caller carries on.  `rememberSuspending` runs
 * `Unconfined`, so it continues on whichever thread resumes it: an OkHttp engine thread, unless fetch hands back on main.
 */
@RunWith(RobolectricTestRunner::class)
class FetchResumeThreadTest {
    private val main: Thread = Looper.getMainLooper().thread

    @Before
    fun setUp() {
        AndroidAppContext.applicationCtx = RuntimeEnvironment.getApplication()
        AndroidAppContext.cookiesStorage = AcceptAllCookiesStorage()
    }

    // Robolectric's main looper only runs posted work when idled, and this test body is its thread.
    private fun <T : Any> pumpMainUntil(get: () -> T?): T {
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            get()?.let { return it }
            Thread.sleep(5)
        }
        error("Timed out")
    }

    @Test
    fun rememberSuspendingContinuesAndNotifiesOnMainAfterFetch() {
        val observed = rememberSuspending {
            fetch("${TestServer.http}/hello").text()
            Thread.currentThread()
        }
        var notifiedOn: Thread? = null
        val release = observed.addListener { notifiedOn = Thread.currentThread() }
        val calculatedOn = pumpMainUntil { observed.state.takeIf { it.success }?.get() }
        release()
        assertEquals(main, calculatedOn)
        assertEquals(main, notifiedOn)
    }

    @Test
    fun unconfinedCallerContinuesOnMainAfterBlob() {
        var after: Thread? = null
        CoroutineScope(Dispatchers.Unconfined).launch {
            fetch("${TestServer.http}/hello").blob()
            after = Thread.currentThread()
        }
        assertEquals(main, pumpMainUntil { after })
    }
}
