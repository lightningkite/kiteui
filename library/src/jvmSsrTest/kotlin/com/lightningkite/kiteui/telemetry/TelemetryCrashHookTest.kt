package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertSame

class TelemetryCrashHookTest {
    @Test
    fun previousHandlerRunsEvenWhenRecordingThrowsAnError() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        var seenByPrevious: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> seenByPrevious = throwable }
        try {
            installCrashHook { throw OutOfMemoryError("while recording") }
            val crash = IllegalStateException("crash")
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), crash)
            assertSame(crash, seenByPrevious)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
        }
    }
}
