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
}
