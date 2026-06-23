package com.lightningkite.mppexampleapp

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Verifies that every `@Routable` documentation page is registered in
 * [com.lightningkite.mppexampleapp.docs.DocSearchPage], so it is reachable from the docs index
 * and search. Pages drift out of the manually-maintained registry list as new ones are added.
 *
 * A DocPage without `@Routable` (only the scaffold `TemplatePage`) is intentionally excluded.
 *
 * JVM-only: it reads Kotlin source from disk.
 */
class DocPagesRegistryTest {

    @Test
    fun everyRoutableDocPageIsRegistered() {
        val docsDir = locateDocsDir()
        val files = docsDir.listFiles { f -> f.extension == "kt" } ?: emptyArray()

        val routableDocPages = files.flatMap { routableDocPageNames(it.readText()) }.toSortedSet()
        check(routableDocPages.isNotEmpty()) { "Found no @Routable DocPage objects under $docsDir." }

        val registered = registeredPageNames(File(docsDir, "DocSearchPage.kt").readText())

        val missing = routableDocPages - registered
        if (missing.isNotEmpty()) {
            fail(
                "These @Routable DocPage(s) are not registered in DocSearchPage.docsPages and are " +
                    "therefore undiscoverable:\n" + missing.joinToString("\n") { "  • $it" } +
                    "\nAdd `{ $missing }`-style entries, or remove @Routable if the page is a scaffold."
            )
        }

        println("✓ All ${routableDocPages.size} @Routable doc pages are registered in DocSearchPage.")
    }

    /** Finds `object X : ... DocPage` declarations preceded by an `@Routable` annotation. */
    private fun routableDocPageNames(src: String): List<String> {
        val regex = Regex("""@Routable\b[^\n]*\)?\s*(?:@\w+[^\n]*\s*)*object\s+(\w+)\s*:[^\n{]*\bDocPage\b""")
        return regex.findAll(src).map { it.groupValues[1] }.toList()
    }

    /** Extracts the `{ PageName }` factory entries from DocSearchPage's docsPages list. */
    private fun registeredPageNames(src: String): Set<String> {
        val list = src.substringAfter("docsPages = Signal(listOf(").substringBefore("))")
        return Regex("""\{\s*(\w+)\s*}""").findAll(list).map { it.groupValues[1] }.toSet()
    }

    private fun locateDocsDir(): File {
        val rel = "src/commonMain/kotlin/com/lightningkite/mppexampleapp/docs"
        var base: File? = File(".").absoluteFile
        repeat(6) {
            base?.let { b ->
                for (candidate in listOf(File(b, rel), File(b, "example-app/$rel"))) {
                    if (candidate.isDirectory) return candidate
                }
            }
            base = base?.parentFile
        }
        error("Could not locate docs source dir ($rel) from ${File(".").absolutePath}")
    }
}
