package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The real Keychain. A Keychain write failure throws with its OSStatus; -34018 would mean this test binary is not
// allowed a Keychain without an app around it.
class SecurePlatformStorageTest {
    @Test
    fun setGetRemove() {
        SecurePlatformStorage.set("round-trip", "first")
        assertEquals("first", SecurePlatformStorage.get("round-trip"))
        SecurePlatformStorage.set("round-trip", "second")
        assertEquals("second", SecurePlatformStorage.get("round-trip"))
        SecurePlatformStorage.remove("round-trip")
        assertNull(SecurePlatformStorage.get("round-trip"))
        SecurePlatformStorage.remove("round-trip")
        assertNull(SecurePlatformStorage.get("never-written"))
    }

    @Test
    fun keysAreIndependentAndValuesKeepEveryCharacter() {
        val value = "naïve ☃ \"quoted\"\nsecond line"
        SecurePlatformStorage.set("a", value, ensureOnDisk = true)
        SecurePlatformStorage.set("b", "other")
        assertEquals(value, SecurePlatformStorage.get("a"))
        SecurePlatformStorage.remove("b")
        assertEquals(value, SecurePlatformStorage.get("a"))
        SecurePlatformStorage.remove("a")
    }
}
