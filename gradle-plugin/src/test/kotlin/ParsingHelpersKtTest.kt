package com.lightningkite.kiteui

import org.junit.Test
import org.junit.Assert.*

class ParsingHelpersKtTest {
    @Test fun test() {
        assertEquals(
            listOf("a", "b", "c"),
            "(a, b, c)".splitParens().map { it.trim() }
        )
        assertEquals(
            listOf("a", "b(d, e)", "c"),
            "(a, b(d, e), c)".splitParens().map { it.trim() }
        )
    }
}