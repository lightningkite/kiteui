package com.lightningkite.kiteui

import kotlin.test.Test

class MicroHtmlTests {
    @Test fun testParsing() {
        val test = """
            <h1>Header</h1>
            <p>
                Paragraph
                <strong>strong</strong>
        """.trimIndent()
    }
}