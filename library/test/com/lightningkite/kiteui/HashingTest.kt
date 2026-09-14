package com.lightningkite.kiteui

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class HashingTest {
    @Test
    fun sha1Test(): Unit {
        runTest {
            assertEquals(
                "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
                "test".encodeToByteArray().sha1().toHexString(),
            )
        }
    }

    @Test
    fun sha256Test(): Unit {
        runTest {
            assertEquals(
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                "test".encodeToByteArray().sha256().toHexString(),
            )
        }
    }

    @Test
    fun sha512Test(): Unit {
        runTest {
            assertEquals(
                "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff",
                "test".encodeToByteArray().sha512().toHexString(),
            )
        }
    }
}