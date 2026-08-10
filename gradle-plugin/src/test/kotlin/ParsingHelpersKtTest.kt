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

    // ---- stripBlockComments -------------------------------------------------------------

    @Test fun `stripBlockComments removes a real block comment`() {
        assertEquals(
            "val x = 5",
            "val x = /* the answer */5".stripBlockComments()
        )
    }

    @Test fun `stripBlockComments ignores comment-like text inside a string literal`() {
        // A "/*" inside a string must not open a phantom comment that swallows
        // everything up to the next real "*/" elsewhere in the file.
        val source = "requestFile(listOf(\"image/*\"))\nval real = /* comment */ 1"
        assertEquals(
            "requestFile(listOf(\"image/*\"))\nval real =  1",
            source.stripBlockComments()
        )
    }

    @Test fun `stripBlockComments ignores comment-like text inside a char literal`() {
        assertEquals(
            "val slash = '/'",
            "val slash = '/'".stripBlockComments()
        )
    }
}