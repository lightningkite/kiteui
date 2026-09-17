package com.lightningkite.kiteui

import android.app.Application
import android.content.Context
import com.lightningkite.kiteui.views.AndroidAppContext
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * The android.jar is stubbed in unit tests (`isReturnDefaultValues`), so a real Context is
 * unavailable without Robolectric. Android's networking reaches the wire through
 * [AndroidAppContext.ktorClient], whose only Context-dependent piece is HttpCache's directory, so
 * supplying a real one is all a unit test needs to exercise the actual client.
 */
private class FakeApplication : Application() {
    private val tempCacheDir: File by lazy {
        File(System.getProperty("java.io.tmpdir"), "kiteui-network-test").apply { mkdirs() }
    }

    override fun getCacheDir(): File = tempCacheDir
    override fun getApplicationContext(): Context = this
}

@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun runPlatformTest(block: suspend CoroutineScope.() -> Unit): TestResult = runBlocking {
    AndroidAppContext.applicationCtx = FakeApplication()
    // WebSocketWrapper delivers every callback on Dispatchers.Main, which a plain JVM test lacks.
    Dispatchers.setMain(Dispatchers.IO)
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
