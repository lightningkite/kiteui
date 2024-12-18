package com.lightningkite.kiteui

import com.lightningkite.kiteui.utils.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class GeneralFormatTest {
    val testCases = listOf(
        "1234567",
        "12345678",
        "123456789",
        "1234.5678",
        "1234.",
        "1,234,567",
        "12,345,678",
        "123,456,789",
        "1,234.56789",
        "1,234.",
    )

    @Test fun deleteComma() {
        // "123,456"
        numberAutocommaRepair("123,456", 4, 4, { println(it) }, { a, b -> println("$a, $b")})
    }

    @Test
    fun editsKeepPosition() {
        fun testOnString(string: String) {
            repeat(string.length + 1) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("--- $string $it Single")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaRepair(
                    string,
                    selectionStart = it,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                string.getOrNull(it)?.takeIf { it == '.' || it.isDigit() }?.let {
                    // Position should be before the comma
                    if(post.getOrNull(postPos) == ',') postPos++
                    assertEquals(it, post.getOrNull(postPos))
                }
            }
            repeat(string.length) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("--- $string $it Span")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaRepair(
                    string,
                    selectionStart = it,
                    selectionEnd = it + 1,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                string.getOrNull(it)?.takeIf { it == '.' || it.isDigit() }?.let {
                    // Position should be before the comma
                    if(post.getOrNull(postPos) == ',') postPos++
                    assertEquals(it, post.getOrNull(postPos))
                }
            }
        }
        testCases.forEach { testOnString(it) }
    }

    @Test
    fun insert() {
        fun testOnString(string: String) {
            repeat(string.length + 1) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("---")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaRepair(
                    string.substring(0, it) + '0' + string.substring(it),
                    selectionStart = it + 1,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                assertEquals('0', post.substring(0, postPos).lastOrNull { it.isDigit() || it == '.' })
            }
        }
        testCases.forEach { testOnString(it) }
    }

    @Test
    fun testBackspace() {
        fun testOnString(string: String) {
            repeat(string.length + 1) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("--- $string $it backspace")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaBackspace(
                    string,
                    selectionStart = it,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                string.getOrNull(it - 1)?.takeIf { it.isDigit() || it == '.' }?.let {
                    if (post.contains(it)) fail()
                }
            }
        }
        testCases.forEach { testOnString(it) }
    }

    @Test
    fun testDelete() {
        fun testOnString(string: String) {
            repeat(string.length + 1) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("--- $string $it delete")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaDelete(
                    string,
                    selectionStart = it,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                string.getOrNull(it)?.takeIf { it.isDigit() || it == '.' }?.let {
                    if (post.contains(it)) fail()
                }
            }
        }
        testCases.forEach { testOnString(it) }
    }

    @Test
    fun testDeleteRange() {
        fun testOnString(string: String) {
            repeat(string.length) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("--- $string $it delete range")
                println(string)
                println("^".padStart(it + 1, ' '))
                numberAutocommaBackspace(
                    string,
                    selectionStart = it,
                    selectionEnd = it + 1,
                    setResult = { post = it; println(it) },
                    setSelectionRange = { a, b ->
                        postPos = a
                        postPosB = b
                        println("^".padStart(a + 1, ' ') + "^".repeat(postPosB - postPos))
                    })
                string.getOrNull(it)?.takeIf { it.isDigit() || it == '.' }?.let {
                    if (post.contains(it)) fail()
                }
            }
        }
        testCases.forEach { testOnString(it) }
    }

    @Test
    fun decimalFormat() {
        assertEquals("5.99", 5.99.toStringNoExponential())
        assertEquals("1224.54", 1224.54.toStringNoExponential())
        assertEquals("1224.542", 1224.542.toStringNoExponential())
        assertEquals("1224.5428", 1224.5428.toStringNoExponential())
        assertEquals("1224.54287", 1224.54287.toStringNoExponential())
        assertEquals("1224.542871", 1224.5428713.toStringNoExponential())
        assertEquals("1224", 1224.0.toStringNoExponential())
        assertEquals("112348224342", 112348224342.0.toStringNoExponential())
    }
}