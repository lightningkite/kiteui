package com.lightningkite.kiteui

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

public class BlobTest {
    @Test fun test() = runTest {
        val c = "This is some test content"
        assertEquals(c, c.toBlob().also {
            println(it.bytes())
        }.toByteArray().also {
            println(it.size)
        }.decodeToString())
    }
}