// by Claude - generates W3C-compliant trace IDs (32 hex chars) and span IDs (16 hex chars)
package com.lightningkite.kiteui.telemetry

import kotlin.random.Random
import kotlin.time.Clock

internal object IdGenerator {
    private val hexChars = "0123456789abcdef"

    /** 32 hex chars (16 bytes) */
    fun traceId(): String = randomHex(32)

    /** 16 hex chars (8 bytes) */
    fun spanId(): String = randomHex(16)

    /** Current time as nanoseconds-since-epoch string, suitable for OTLP timestamps. */
    fun nanosString(): String {
        val millis = Clock.System.now().toEpochMilliseconds()
        return (millis * 1_000_000L).toString()
    }

    private fun randomHex(length: Int): String {
        val bytes = Random.nextBytes(length / 2)
        return buildString(length) {
            for (b in bytes) {
                append(hexChars[(b.toInt() shr 4) and 0xf])
                append(hexChars[b.toInt() and 0xf])
            }
        }
    }
}
