package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorReporterTest {
    @Test
    fun reportReachesRegisteredReportersUntilReleased() {
        val seen = mutableListOf<Pair<String?, String>>()
        val release = ThrowableReporter.global.register(ThrowableReporter { throwable, context -> seen += throwable.message to context })
        try {
            Exception("first").report("here")
        } finally {
            release()
        }
        Exception("second").report("there")
        assertEquals(listOf<Pair<String?, String>>("first" to "here"), seen)
    }
}
