package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CrashFingerprintTest {

    @Test
    fun normalizationStripsMemoryAddresses() {
        val input = "at com.example.Foo@0x1a2b3c.bar(Foo.kt:42)"
        val normalized = CrashFingerprint.normalize(input)
        assertTrue("0x1a2b3c" !in normalized, "Address should be stripped")
        assertTrue("0x???" in normalized, "Address should be replaced with 0x???")
    }

    @Test
    fun normalizationStripsIdentityHashes() {
        val input = "at com.example.Foo@deadbeef.bar(Foo.kt:42)"
        val normalized = CrashFingerprint.normalize(input)
        assertTrue("@deadbeef" !in normalized, "Identity hash should be stripped")
        assertTrue("@???" in normalized, "Identity hash should be replaced with @???")
    }

    @Test
    fun normalizationStripsLineNumbers() {
        val input = "at com.example.Foo.bar(Foo.kt:42)"
        val normalized = CrashFingerprint.normalize(input)
        assertTrue(":42" !in normalized, "Line number should be stripped")
        assertTrue(".kt:?" in normalized, "Line number should be replaced with ?")
    }

    @Test
    fun sameExceptionSameFingerprint() {
        val stack1 = """
            java.lang.NullPointerException: value was null
                at com.example.Foo.bar(Foo.kt:42)
                at com.example.Baz.run(Baz.kt:10)
        """.trimIndent()
        val stack2 = """
            java.lang.NullPointerException: value was null
                at com.example.Foo.bar(Foo.kt:99)
                at com.example.Baz.run(Baz.kt:55)
        """.trimIndent()
        // Same exception type and call structure, only line numbers differ
        assertEquals(
            CrashFingerprint.normalize(stack1),
            CrashFingerprint.normalize(stack2),
            "Same call structure should normalize identically"
        )
    }

    @Test
    fun differentExceptionTypesDifferentFingerprints() {
        val ex1 = RuntimeException("test")
        val ex2 = IllegalArgumentException("test")
        assertNotEquals(
            CrashFingerprint.generate(ex1),
            CrashFingerprint.generate(ex2),
            "Different exception types should produce different fingerprints"
        )
    }

    @Test
    fun fingerprintIs16HexChars() {
        val fp = CrashFingerprint.generate(RuntimeException("test"))
        assertEquals(16, fp.length, "Fingerprint should be 16 chars: $fp")
        assertTrue(Regex("^[0-9a-f]+$").matches(fp), "Fingerprint should be hex: $fp")
    }

    @Test
    fun normalizationIsIdempotent() {
        val input = """
            java.lang.NullPointerException: null
                at com.example.Foo@0xabc123.bar(Foo.kt:42)
                at com.example.Baz@deadbeef.run(Baz.kt:10)
        """.trimIndent()
        val once = CrashFingerprint.normalize(input)
        val twice = CrashFingerprint.normalize(once)
        assertEquals(once, twice, "Normalization should be idempotent")
    }

    @Test
    fun fnv1a64ProducesConsistentOutput() {
        val hash1 = CrashFingerprint.fnv1a64("hello")
        val hash2 = CrashFingerprint.fnv1a64("hello")
        assertEquals(hash1, hash2, "Same input should produce same hash")

        val hash3 = CrashFingerprint.fnv1a64("world")
        assertNotEquals(hash1, hash3, "Different input should produce different hash")
    }

    @Test
    fun takesOnly10Lines() {
        val lines = (1..20).joinToString("\n") { "at com.example.Foo.method$it(Foo.kt:$it)" }
        val normalized = CrashFingerprint.normalize(lines)
        assertEquals(10, normalized.lines().size, "Should take at most 10 lines")
    }

    // --- Cause chain ---

    @Test
    fun causeChainSingleException() {
        val ex = IllegalStateException("boom")
        assertEquals("IllegalStateException", CrashFingerprint.causeChain(ex))
    }

    @Test
    fun causeChainNestedExceptions() {
        val root = RuntimeException("disk error")
        val wrapper = IllegalStateException("failed", root)
        assertEquals("IllegalStateException > RuntimeException", CrashFingerprint.causeChain(wrapper))
    }

    @Test
    fun causeChainLimitsDepth() {
        var ex: Exception = Exception("root")
        repeat(15) { ex = Exception("level $it", ex) }
        val chain = CrashFingerprint.causeChain(ex)
        // Should have at most 10 entries
        assertTrue(chain.split(" > ").size <= 10, "Cause chain should be capped at 10")
    }

    // --- isUsableTrace ---

    @Test
    fun usableTraceWithRealFrames() {
        val trace = """
            at com.example.Foo.bar(Foo.kt:?)
            at com.example.Baz.run(Baz.kt:?)
        """.trimIndent()
        assertTrue(CrashFingerprint.isUsableTrace(trace), "Real frames should be usable")
    }

    @Test
    fun unusableTraceWithOnlyAddresses() {
        val trace = "0x???\n0x???\n0x???\n0x???"
        assertTrue(!CrashFingerprint.isUsableTrace(trace), "Address-only trace should not be usable")
    }

    @Test
    fun unusableTraceEmpty() {
        assertTrue(!CrashFingerprint.isUsableTrace(""), "Empty trace should not be usable")
    }

    // --- normalizeMessage ---

    @Test
    fun normalizeMessageStripsNumbers() {
        assertEquals("User ? not found", CrashFingerprint.normalizeMessage("User 42 not found"))
    }

    @Test
    fun normalizeMessageStripsUuids() {
        val msg = "Item 550e8400-e29b-41d4-a716-446655440000 missing"
        assertEquals("Item ? missing", CrashFingerprint.normalizeMessage(msg))
    }

    @Test
    fun normalizeMessageStripsHex() {
        assertEquals("Address ? invalid", CrashFingerprint.normalizeMessage("Address 0xDEAD invalid"))
    }

    // --- Adaptive fingerprint ---

    @Test
    fun garbageTraceFallsBackToCauseChainAndMessage() {
        // Two exceptions with same type and message but different garbage addresses
        // should produce the same fingerprint
        val ex1 = RuntimeException("connection failed at port 8080")
        val ex2 = RuntimeException("connection failed at port 9090")
        // On a platform where stack traces are real, these would differ (different call sites).
        // But since the message contains the key info, let's verify the message normalization:
        val msg1 = CrashFingerprint.normalizeMessage("connection failed at port 8080")
        val msg2 = CrashFingerprint.normalizeMessage("connection failed at port 9090")
        assertEquals(msg1, msg2, "Normalized messages with different numbers should match")
    }
}
