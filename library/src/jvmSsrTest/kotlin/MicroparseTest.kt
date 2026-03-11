package com.lightningkite.kiteui.dom

import kotlin.test.Test
import kotlin.test.assertEquals

class MicroparseTest {

    @Test fun secure() {
        val base = """
            <script> TEE HEE HEE </script>
            <button onclick="DIE">HEEE HEE</button>
            <p> Test <br> Content </p>
        """.trimIndent().parseMPNodes()
        base.let { println(it) }
        base.forEach { it.secure() }
        base.let { println(it) }
    }

    @Test fun malformed() {
        val base = """
            Something <
        """.trimIndent().parseMPNodes()
        base.let { println(it) }
        base.forEach { it.secure() }
        base.let { println(it) }
    }

    @Test fun malformed2() {
        val base = """
            Yeah, it's a special <p> Hello World! </
        """.trimIndent().parseMPNodes()
        base.let { println(it) }
        base.forEach { it.secure() }
        base.let { println(it) }
    }

    @Test fun full() {
        """
            <div class="UF5u5" other = "x" otheasr = "x" yet="another"><div class="ZHUJ9 _2SqQ">All the errors detected are listed below, from left to right, as they appear in the pattern.</div><div><span class="KrSXE w_XEF YmAvV">/</span> <span>An unescaped delimiter must be escaped; in most languages with a backslash (<span class="T0laQ"><span>\</span></span>)</span></div><div><span class="KrSXE w_XEF YmAvV">?</span> The preceding token is not quantifiable</div><div><span class="KrSXE w_XEF YmAvV">/</span> <span>An unescaped delimiter must be escaped; in most languages with a backslash (<span class="T0laQ"><span>\</span></span>)</span></div><div><span class="KrSXE w_XEF YmAvV">?</span> The preceding token is not quantifiable</div></div>
        """.trimIndent().parseMPNodes().let { println(it) }
    }

    @Test fun simple() {
        """
            <p>Test</p>
        """.trimIndent().parseMPNodes().let { println(it) }
    }

    @Test
    fun testPrinting() {
        """
                        <p>Test</p>
        """.trimIndent().starts(
            onTag = {
                it.analyzeTagInside { tagName, start, end, kvs ->
                    println("TAG: $tagName $start $end $kvs")
                }
            },
            onContent = { println("CONTENT: $it") },
        )
    }

    @Test
    fun testTagAnalysis() {
        "div".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(false, end)
            assertEquals(mapOf(), kvs)
        }
        "/ div".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(false, start)
            assertEquals(true, end)
            assertEquals(mapOf(), kvs)
        }
        "/div".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(false, start)
            assertEquals(true, end)
            assertEquals(mapOf(), kvs)
        }
        "div/".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(true, end)
            assertEquals(mapOf(), kvs)
        }
        "div attr".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(false, end)
            assertEquals(mapOf("attr" to ""), kvs)
        }
        "div attr='x'".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(false, end)
            assertEquals(mapOf("attr" to "x"), kvs)
        }
        "div attr='x' attr2".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(false, end)
            assertEquals(mapOf("attr" to "x", "attr2" to ""), kvs)
        }
        "div attr='x' attr2 /".analyzeTagInside { tagName, start, end, kvs ->
            assertEquals("div", tagName)
            assertEquals(true, start)
            assertEquals(true, end)
            assertEquals(mapOf("attr" to "x", "attr2" to ""), kvs)
        }
    }
}