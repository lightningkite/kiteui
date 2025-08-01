package com.lightningkite.kiteui

import kotlin.test.Test

public class MicroHtmlTests {
    @Test fun testParsing() {
        public val test = """
            <h1>Header</h1>
            <p>
                Paragraph
                <strong>strong</strong>
        """.trimIndent()
    }
}