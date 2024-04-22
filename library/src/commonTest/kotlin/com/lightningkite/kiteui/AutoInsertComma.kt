package com.lightningkite.kiteui

import com.lightningkite.kiteui.utils.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class AutoInsertComma {
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
    @Test
    fun editsKeepPosition() {
        fun testOnString(string: String) {
            repeat(string.length + 1) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("---")
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
                    assertEquals(it, post.getOrNull(postPos))
                }
            }
            repeat(string.length) {
                var post = ""
                var postPos: Int = 0
                var postPosB: Int = 0
                println("---")
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
                assertEquals('0', post.substring(0, postPos).lastOrNull { it.isDigit() ||  it == '.' })
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
                println("---")
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
                    if(post.contains(it)) fail()
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
                println("---")
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
                string.getOrNull(it) ?.takeIf { it.isDigit() || it == '.' }?.let {
                    if(post.contains(it)) fail()
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
                println("---")
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
                    if(post.contains(it)) fail()
                }
            }
        }
        testCases.forEach { testOnString(it) }
    }
}