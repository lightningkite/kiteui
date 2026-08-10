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

    @Test fun `stripBlockComments handles comment-like patterns inside comments`() {
        // Block comments don't nest in Kotlin - the first */ closes the comment
        // So `/* outer /* nested */` is treated as one comment that ends at the first */
        assertEquals(
            "val x =  1\nval y = 2",
            "val x = /* outer /* nested */ 1\nval y = 2".stripBlockComments()
        )
    }

    @Test fun `stripBlockComments preserves triple-quoted strings with comment-like content`() {
        val source = """val doc = ""${'"'}This is /* not a comment */""${'"'}"""
        assertEquals(source, source.stripBlockComments())
    }

    // ---- stripLineComments -------------------------------------------------------------

    @Test fun `stripLineComments removes a simple line comment`() {
        assertEquals(
            "val x = 5\n",
            "val x = 5 // this is a comment\n".stripLineComments()
        )
    }

    @Test fun `stripLineComments removes line comment at start of line`() {
        assertEquals(
            "\nval x = 5",
            "// comment at start\nval x = 5".stripLineComments()
        )
    }

    @Test fun `stripLineComments removes multiple line comments`() {
        assertEquals(
            "val x = 5\nval y = 10\n",
            "val x = 5 // first comment\nval y = 10 // second comment\n".stripLineComments()
        )
    }

    @Test fun `stripLineComments preserves double-slash inside string literals`() {
        assertEquals(
            "val url = \"http://example.com\"",
            "val url = \"http://example.com\"".stripLineComments()
        )
    }

    @Test fun `stripLineComments preserves double-slash inside char literal`() {
        assertEquals(
            "val slash = '/'",
            "val slash = '/'".stripLineComments()
        )
    }

    @Test fun `stripLineComments handles inline comment after string with double-slash`() {
        assertEquals(
            "val url = \"http://example.com\"\n",
            "val url = \"http://example.com\" // URL comment\n".stripLineComments()
        )
    }

    @Test fun `stripLineComments preserves newlines`() {
        val source = "val x = 1 // comment\nval y = 2\nval z = 3 // another comment\n"
        val result = source.stripLineComments()
        assertEquals(3, result.count { it == '\n' })
    }

    @Test fun `stripLineComments handles triple-quoted strings with double-slash`() {
        val source = """val doc = ""${'"'}URL: http://example.com""${'"'}"""
        assertEquals(source, source.stripLineComments())
    }

    @Test fun `stripLineComments handles escaped quotes correctly`() {
        assertEquals(
            "val str = \"He said \\\"yes\\\"\"\n",
            "val str = \"He said \\\"yes\\\"\" // comment\n".stripLineComments()
        )
    }

    @Test fun `stripLineComments with Windows line endings`() {
        assertEquals(
            "val x = 5\r\n",
            "val x = 5 // comment\r\n".stripLineComments()
        )
    }

    // ---- stripComments (combination of both) --------------------------------------------

    @Test fun `stripComments removes both line and block comments`() {
        val source = """
            val x = /* block */ 5 // line comment
            val y = 10 /* another block */
        """.trimIndent()
        // Block comments preserve surrounding whitespace (inline behavior)
        // Line comments remove trailing whitespace before the comment
        val expected = "val x =  5\nval y = 10 "
        assertEquals(expected, source.stripComments())
    }

    @Test fun `stripComments handles complex real-world code`() {
        val source = """
            @Routable("items/{id}") // Route annotation
            class ItemPage(val id: String) { /* constructor params */
                @QueryParameter // Query param
                val filter: String = "" /* default value */
                fun load() {
                    val url = "http://api.example.com" // API endpoint
                }
            }
        """.trimIndent()
        val result = source.stripComments()

        // Verify annotation is preserved
        assertTrue("@Routable annotation should be preserved", "@Routable" in result)
        assertTrue("@QueryParameter annotation should be preserved", "@QueryParameter" in result)

        // Verify string literal with // is preserved
        assertTrue("URL with // should be preserved", "http://api.example.com" in result)

        // Verify comments are removed
        assertFalse("Line comment should be removed", "Route annotation" in result)
        assertFalse("Block comment should be removed", "constructor params" in result)
        assertFalse("Another line comment should be removed", "Query param" in result)
    }

    @Test fun `stripComments handles nested string interpolation`() {
        val source = """val msg = "URL: ${'$'}{baseUrl}//path" // comment"""
        val result = source.stripComments()
        assertTrue("Interpolated string should be preserved", "//path" in result)
        assertFalse("Comment should be removed", "// comment" in result)
    }

    @Test fun `stripComments preserves MIME types with slash-star`() {
        val source = """
            val types = listOf("image/*") // Image types
            val x = /* comment */ 5
        """.trimIndent()
        val result = source.stripComments()
        assertTrue("MIME type should be preserved", "image/*" in result)
        assertFalse("Comments should be removed", "Image types" in result)
        assertFalse("Block comment should be removed", "/* comment */" in result)
    }
}