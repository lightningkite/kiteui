// by Claude - tests for IdGenerator: trace/span ID format and timestamp generation
package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdGeneratorTest {
    private val hexPattern = Regex("^[0-9a-f]+$")

    @Test
    fun traceIdIs32HexChars() {
        val id = IdGenerator.traceId()
        assertEquals(32, id.length, "Trace ID should be 32 chars")
        assertTrue(hexPattern.matches(id), "Trace ID should be lowercase hex: $id")
    }

    @Test
    fun spanIdIs16HexChars() {
        val id = IdGenerator.spanId()
        assertEquals(16, id.length, "Span ID should be 16 chars")
        assertTrue(hexPattern.matches(id), "Span ID should be lowercase hex: $id")
    }

    @Test
    fun traceIdsAreUnique() {
        val ids = (1..100).map { IdGenerator.traceId() }.toSet()
        assertEquals(100, ids.size, "100 trace IDs should all be unique")
    }

    @Test
    fun spanIdsAreUnique() {
        val ids = (1..100).map { IdGenerator.spanId() }.toSet()
        assertEquals(100, ids.size, "100 span IDs should all be unique")
    }

    @Test
    fun nanosStringIsPositiveNumber() {
        val nanos = IdGenerator.nanosString()
        val parsed = nanos.toLongOrNull()
        assertTrue(parsed != null && parsed > 0, "nanosString should be a positive long: $nanos")
    }

    @Test
    fun nanosStringIsReasonableEpoch() {
        val nanos = IdGenerator.nanosString().toLong()
        // Should be after 2024-01-01 in nanoseconds
        val jan2024Nanos = 1704067200L * 1_000_000_000L
        assertTrue(nanos > jan2024Nanos, "Timestamp should be after 2024-01-01")
    }
}
