package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdGeneratorTest {
    private val hexPattern = Regex("^[0-9a-f]+$")

    @Test
    fun traceIdIs32HexChars() {
        val id = Telemetry.traceId()
        assertEquals(32, id.length, "Trace ID should be 32 chars")
        assertTrue(hexPattern.matches(id), "Trace ID should be lowercase hex: $id")
    }

    @Test
    fun spanIdIs16HexChars() {
        val id = Telemetry.spanId()
        assertEquals(16, id.length, "Span ID should be 16 chars")
        assertTrue(hexPattern.matches(id), "Span ID should be lowercase hex: $id")
    }

    @Test
    fun traceIdsAreUnique() {
        val ids = (1..100).map { Telemetry.traceId() }.toSet()
        assertEquals(100, ids.size, "100 trace IDs should all be unique")
    }

    @Test
    fun spanIdsAreUnique() {
        val ids = (1..100).map { Telemetry.spanId() }.toSet()
        assertEquals(100, ids.size, "100 span IDs should all be unique")
    }

    @Test
    fun nanosStringIsPositiveNumber() {
        val nanos = Telemetry.nanosString()
        val parsed = nanos.toLongOrNull()
        assertTrue(parsed != null && parsed > 0, "nanosString should be a positive long: $nanos")
    }

    @Test
    fun nanosStringIsReasonableEpoch() {
        val nanos = Telemetry.nanosString().toLong()
        // Should be after 2024-01-01 in nanoseconds
        val jan2024Nanos = 1704067200L * 1_000_000_000L
        assertTrue(nanos > jan2024Nanos, "Timestamp should be after 2024-01-01")
    }
}
