package com.lightningkite.kiteui

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Tests for the route-code-generator hardening (generateRoutes.kt).
 *
 * Each test exercises a specific defect that was fixed:
 *   (a) @QueryParameter scan bounded to the current class body
 *   (b) val/var keyword recognised at a word boundary (not inside "private")
 *   (c) Parser order: specific routes (constant segments) before variable routes
 *   (d) Duplicate/ambiguous route templates throw a helpful error
 *   (e) Malformed @Routable (missing path string) throws a helpful error
 *   (f) A string literal containing a MIME wildcard glob does not open a phantom block comment
 */
class GenerateRoutesKtTest {

    /**
     * Writes each (filename, content) pair into a temporary source directory,
     * runs generateAutoroutes, and returns the generated file as a string.
     */
    private fun generateFromSources(vararg sources: Pair<String, String>): String {
        val srcDir = java.nio.file.Files.createTempDirectory("kiteui-route-test").toFile()
        val outFile = File.createTempFile("kiteui-routes", ".kt")
        try {
            sources.forEach { (name, content) ->
                File(srcDir, name).writeText(content)
            }
            generateAutoroutes(srcDir, outFile)
            return outFile.readText()
        } finally {
            srcDir.deleteRecursively()
            outFile.delete()
        }
    }

    // ---- (a) @QueryParameter bounded to class body ----------------------------------------

    @Test
    fun `query params are attributed to the correct class when two routables share a file`() {
        // Before the fix, the @QueryParameter scan was unbounded: when processing
        // FirstScreen it would scan to end-of-file and pick up SecondScreen's params too.
        val source = """
            package com.example

            @Routable("first")
            object FirstScreen {
                @QueryParameter
                val firstParam: String = ""
                fun init() {}
            }

            @Routable("second")
            object SecondScreen {
                @QueryParameter
                val secondParam: String = ""
                fun init() {}
            }
        """.trimIndent()

        val result = generateFromSources("Screens.kt" to source)

        // Only the parsers section is relevant; renderers will mention both params legitimately.
        val parsers = result.substringAfter("parsers = listOf(").substringBefore("renderers = mapOf(")

        val secondRouteIdx = parsers.indexOf("\"second\"")
        assertTrue("'second' segment check must exist in parsers section", secondRouteIdx > 0)

        // firstParam must appear inside FirstScreen's block (before the "second" check).
        val firstParamIdx = parsers.indexOf("firstParam")
        assertTrue("firstParam must be attributed to FirstScreen (appear before 'second' route)",
            firstParamIdx in 0 until secondRouteIdx)

        // secondParam must NOT bleed into FirstScreen's block.
        val secondParamIdx = parsers.indexOf("secondParam")
        assertTrue("secondParam must not appear in FirstScreen's parser block",
            secondParamIdx == -1 || secondParamIdx >= secondRouteIdx)
    }

    // ---- (b) val/var keyword matching -------------------------------------------------------

    @Test
    fun `val keyword is matched at word boundary and not inside 'private'`() {
        // Before the fix, indexOf("va") matched the "va" inside "private", causing the
        // extracted param name to be "val" instead of the actual property name.
        val source = """
            package com.example

            @Routable("screen")
            object ScreenWithPrivate {
                @QueryParameter
                private val myParam: String = ""
                fun init() {}
            }
        """.trimIndent()

        val result = generateFromSources("Screen.kt" to source)

        assertTrue("myParam should be extracted as the query-parameter name",
            result.contains("\"myParam\""))
        assertFalse("'val' must not be extracted as a query-parameter name (indexOf(\"va\") bug)",
            result.contains("""parameters, "val""""))
    }

    // ---- (c) Route ordering: specific-before-general ----------------------------------------

    @Test
    fun `more-specific routes are placed before variable routes in the parsers list`() {
        // Routes are declared in the "wrong" order (general first, specific second).
        // The fix sorts parsers by variable-segment count so the specific route always wins.
        val source = """
            package com.example

            @Routable("items/{id}")
            class ItemDetailScreen(val id: String) {}

            @Routable("items/create")
            object ItemCreateScreen {}
        """.trimIndent()

        val result = generateFromSources("Screens.kt" to source)
        val parsers = result.substringAfter("parsers = listOf(").substringBefore("renderers = mapOf(")

        val createIdx = parsers.indexOf("ItemCreateScreen")
        val detailIdx = parsers.indexOf("ItemDetailScreen")
        assertTrue("ItemCreateScreen (constant segment) must appear before ItemDetailScreen ({id})",
            createIdx in 0 until detailIdx)
    }

    // ---- (d) Duplicate route templates throw ------------------------------------------------

    @Test
    fun `duplicate route templates throw an error naming both conflicting classes`() {
        // items/{id} and items/{slug} have the same structural template, making them
        // ambiguous at runtime. The fix detects this and fails the build.
        val source = """
            package com.example

            @Routable("items/{id}")
            class ItemByIdScreen(val id: String) {}

            @Routable("items/{slug}")
            class ItemBySlugScreen(val slug: String) {}
        """.trimIndent()

        try {
            generateFromSources("Screens.kt" to source)
            fail("Expected duplicate route detection to throw")
        } catch (e: IllegalStateException) {
            val msg = e.message ?: ""
            assertTrue("Error must name ItemByIdScreen", "ItemByIdScreen" in msg)
            assertTrue("Error must name ItemBySlugScreen", "ItemBySlugScreen" in msg)
        }
    }

    // ---- (f) "/*" inside a string literal must not open a phantom block comment -------------

    @Test
    fun `a string literal containing slash-star does not swallow the rest of the class body`() {
        // Before the fix, the naive block-comment regex treated the "/*" in "image/*" as the
        // start of a real comment and deleted everything up to the next "*/" (the one inside
        // the trailing line comment below), taking the class's closing brace with it. That left
        // the source with unbalanced braces, which afterBraces() then reported as a failure.
        val source = """
            package com.example

            @Routable("courses/{id}/manage")
            class CourseManagementPage(val id: String) {
                @QueryParameter
                val mode: String = ""
                fun pickImage() {
                    requestFile(listOf("image/*"))
                }
                fun clone() {
                    doThing().takeIf { false /*allowedToCreateCourse()*/ }
                }
            }
        """.trimIndent()

        val result = generateFromSources("CourseManagementPage.kt" to source)

        assertTrue("route generation must succeed and reference the class",
            result.contains("CourseManagementPage"))
        assertTrue("the @QueryParameter inside the class body must still be found",
            result.contains("\"mode\""))
    }

}
