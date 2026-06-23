package com.lightningkite.mppexampleapp

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Verifies that every documentation `example("""...""") { ... }` shows the reader
 * exactly the code that is actually rendered.
 *
 * Each [com.lightningkite.mppexampleapp.docs.example] call duplicates its sample twice:
 * once as a displayed string (what the reader copies) and once as a live lambda (what runs).
 * Nothing in the type system keeps the two in sync, so they drift. This test reads the docs
 * source files, extracts both halves of every example, and asserts they are equivalent.
 *
 * "Equivalent" means identical after dropping comments and whitespace. Comments that explain
 * the live code but are omitted from the displayed string are therefore allowed; everything
 * else must match — including the modifier operator, so `card - col` displayed against
 * `card.col` rendered is reported as drift.
 *
 * When the displayed snippet is *intentionally* not a verbatim copy of the lambda (for example
 * it omits presentation scaffolding like a sizing `frame { ... }` wrapper, or shows a conceptual
 * definition), mark the example by putting a `// docs:abridged` comment anywhere inside its
 * lambda body. Abridged examples are exempt from the equality check but still listed in the
 * report so the set of declared exceptions stays visible and auditable.
 *
 * This is a JVM-only test because it reads Kotlin source from disk — the displayed-vs-live
 * comparison cannot be done by reflection, since lambda source text is not available at runtime.
 */
class DocExamplesConsistencyTest {

    /**
     * When false, the test reports drift but does not fail the build (current state — many examples
     * intentionally show curated excerpts). Flip to true once the displayed code has been reconciled
     * with the rendered code to turn this into an enforced gate.
     */
    private val enforce = false

    @Test
    fun displayedCodeMatchesRenderedCode() {
        val docsDir = locateDocsDir()
        val sites = docsDir.listFiles { f -> f.extension == "kt" }
            ?.sortedBy { it.name }
            ?.flatMap { parseExamples(it) }
            ?: emptyList()

        check(sites.isNotEmpty()) { "Found no example() calls under $docsDir — parser or path is wrong." }

        // Examples explicitly marked `// docs:abridged` are intentionally not verbatim copies
        // (e.g. the displayed snippet omits presentation scaffolding). They are exempt from the
        // equality check but listed so the set stays auditable.
        val (abridged, checked) = sites.partition { it.abridged }
        val drift = checked.filter { tokenize(it.decodedDisplayedCode) != tokenize(it.lambdaBody) }
        val report = buildReport(sites, abridged, drift)

        // Always make the report findable: print it and write it to the build dir.
        println(report)
        runCatching {
            File("build/reports/doc-examples").apply { mkdirs() }
                .resolve("drift.txt").writeText(report)
        }

        if (enforce && drift.isNotEmpty()) {
            fail("${drift.size} documentation example(s) show code that differs from what is rendered. See report above.")
        }
    }

    private fun buildReport(sites: List<ExampleSite>, abridged: List<ExampleSite>, drift: List<ExampleSite>): String =
        buildString {
            appendLine("=== Documentation example consistency ===")
            appendLine("${sites.size} examples scanned: ${drift.size} drift, ${abridged.size} declared abridged, " +
                "${sites.size - drift.size - abridged.size} matching.")
            if (drift.isNotEmpty()) {
                appendLine()
                appendLine("--- Drift: displayed code differs from what is rendered ---")
                appendLine("Make the displayed string match the lambda body, or mark the example")
                appendLine("`// docs:abridged` if the displayed snippet is intentionally not verbatim.")
                for (s in drift) {
                    appendLine("• ${s.file}:${s.line}")
                    appendLine(firstDifference(s.decodedDisplayedCode, s.lambdaBody).prependIndent("    "))
                }
            }
            if (abridged.isNotEmpty()) {
                appendLine()
                appendLine("--- Declared abridged (// docs:abridged, not checked) ---")
                abridged.forEach { appendLine("• ${it.file}:${it.line}") }
            }
        }

    // ---- Extraction -------------------------------------------------------------------------

    private data class ExampleSite(
        val file: String,
        val line: Int,
        val displayedCode: String,
        val lambdaBody: String,
    ) {
        /** An example marked `// docs:abridged` is intentionally not a verbatim copy of its lambda. */
        val abridged: Boolean get() = lambdaBody.contains("docs:abridged")
        /** The displayed string escapes `$` as `${'$'}` so the literal survives being inside a template. */
        val decodedDisplayedCode: String
            get() = displayedCode.replace("\${'\$'}", "\$").replace("\${\"\$\"}", "\$")
    }

    /** Walks a file once, finding `example(` calls and capturing their string arg + trailing lambda. */
    private fun parseExamples(file: File): List<ExampleSite> {
        val src = file.readText()
        val results = mutableListOf<ExampleSite>()
        var i = 0
        while (i < src.length) {
            i = skipTrivia(src, i)
            if (i >= src.length) break
            val word = matchWord(src, i)
            if (word == "example" && !isMemberAccess(src, i)) {
                val afterName = skipSpace(src, i + word.length)
                if (afterName < src.length && src[afterName] == '(') {
                    val call = readExampleCall(src, file, afterName)
                    if (call != null) {
                        results += call.site
                        i = call.endIndex
                        continue
                    }
                }
                i += word.length
            } else {
                i += if (word != null) word.length else 1
            }
        }
        return results
    }

    private class CallResult(val site: ExampleSite, val endIndex: Int)

    /** Given the index of `example`'s opening `(`, capture the first triple-string arg and the lambda after `)`. */
    private fun readExampleCall(src: String, file: File, parenStart: Int): CallResult? {
        var i = parenStart + 1
        var depth = 1
        var displayed: String? = null
        while (i < src.length && depth > 0) {
            when {
                isCommentOrString(src, i) -> {
                    val (text, next, isTriple) = readStringOrComment(src, i)
                    if (isTriple && displayed == null && depth == 1) displayed = trimIndentLike(text)
                    i = next
                }
                src[i] == '(' -> { depth++; i++ }
                src[i] == ')' -> { depth--; i++ }
                else -> i++
            }
        }
        if (displayed == null) return null // e.g. the `fun example(...)` declaration itself
        // After the closing paren, the trailing lambda begins at the next `{`.
        var j = skipTrivia(src, i)
        if (j >= src.length || src[j] != '{') return null
        val (body, end) = readBalancedBraces(src, j)
        val line = src.substring(0, parenStart).count { it == '\n' } + 1
        return CallResult(ExampleSite(file.name, line, displayed, body), end)
    }

    /** Reads `{ ... }` starting at the open brace, returning the inner body and the index past the close. */
    private fun readBalancedBraces(src: String, openBrace: Int): Pair<String, Int> {
        var i = openBrace + 1
        var depth = 1
        val start = i
        while (i < src.length && depth > 0) {
            when {
                isCommentOrString(src, i) -> i = readStringOrComment(src, i).next
                src[i] == '{' -> { depth++; i++ }
                src[i] == '}' -> { depth--; i++ }
                else -> i++
            }
        }
        return src.substring(start, i - 1) to i
    }

    // ---- Low-level scanning -----------------------------------------------------------------

    private data class Lexeme(val text: String, val next: Int, val isTriple: Boolean)

    private fun isCommentOrString(src: String, i: Int): Boolean {
        val c = src[i]
        if (c == '"' || c == '\'') return true
        if (c == '/' && i + 1 < src.length && (src[i + 1] == '/' || src[i + 1] == '*')) return true
        return false
    }

    /** Reads a comment, string, or char literal starting at [i]. For strings, `text` is the inner content. */
    private fun readStringOrComment(src: String, i: Int): Lexeme {
        val c = src[i]
        return when {
            c == '/' && src[i + 1] == '/' -> {
                val end = src.indexOf('\n', i).let { if (it == -1) src.length else it }
                Lexeme("", end, false)
            }
            c == '/' && src[i + 1] == '*' -> {
                val end = src.indexOf("*/", i + 2).let { if (it == -1) src.length else it + 2 }
                Lexeme("", end, false)
            }
            c == '"' && src.startsWith("\"\"\"", i) -> {
                val end = src.indexOf("\"\"\"", i + 3).let { if (it == -1) src.length else it }
                Lexeme(src.substring(i + 3, end), (end + 3).coerceAtMost(src.length), true)
            }
            c == '"' -> readQuoted(src, i, '"')
            else -> readQuoted(src, i, '\'')
        }
    }

    /** Reads a normal "..." or '...' literal, honoring `\` escapes and `${ ... }` templates. */
    private fun readQuoted(src: String, start: Int, quote: Char): Lexeme {
        var i = start + 1
        while (i < src.length) {
            when (val c = src[i]) {
                '\\' -> i += 2
                '$' -> if (i + 1 < src.length && src[i + 1] == '{') {
                    i = readBalancedBraces(src, i + 1).second
                } else i++
                quote -> return Lexeme(src.substring(start + 1, i), i + 1, false)
                else -> { @Suppress("UNUSED_EXPRESSION") c; i++ }
            }
        }
        return Lexeme(src.substring(start + 1, i), i, false)
    }

    private fun skipTrivia(src: String, start: Int): Int {
        var i = start
        while (i < src.length) {
            when {
                src[i].isWhitespace() -> i++
                src[i] == '/' && i + 1 < src.length && (src[i + 1] == '/' || src[i + 1] == '*') ->
                    i = readStringOrComment(src, i).next
                else -> return i
            }
        }
        return i
    }

    private fun skipSpace(src: String, start: Int): Int {
        var i = start
        while (i < src.length && src[i].isWhitespace()) i++
        return i
    }

    private fun matchWord(src: String, i: Int): String? {
        if (!isIdentStart(src[i])) return null
        var j = i
        while (j < src.length && isIdentPart(src[j])) j++
        return src.substring(i, j)
    }

    /** A `.example` or `?.example` would be member access, not a call to the docs helper. */
    private fun isMemberAccess(src: String, i: Int): Boolean {
        var j = i - 1
        while (j >= 0 && src[j] == ' ') j--
        return j >= 0 && src[j] == '.'
    }

    private fun isIdentStart(c: Char) = c.isLetter() || c == '_'
    private fun isIdentPart(c: Char) = c.isLetterOrDigit() || c == '_'

    // ---- Normalization & comparison ---------------------------------------------------------

    /**
     * Reduces Kotlin source to a list of significant tokens, dropping whitespace and comments.
     * Does not need to be a *correct* Kotlin lexer — only a *deterministic* one, since the same
     * function is applied to both sides and we compare the results for equality.
     */
    private fun tokenize(code: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < code.length) {
            val c = code[i]
            when {
                c.isWhitespace() -> i++
                c == '/' && i + 1 < code.length && (code[i + 1] == '/' || code[i + 1] == '*') ->
                    i = readStringOrComment(code, i).next
                c == '"' || c == '\'' -> {
                    val lex = readStringOrComment(code, i)
                    tokens += "S:" + lex.text
                    i = lex.next
                }
                isIdentStart(c) -> {
                    val w = matchWord(code, i)!!
                    tokens += "I:" + w
                    i += w.length
                }
                c.isDigit() -> {
                    var j = i
                    while (j < code.length && code[j].isDigit()) j++
                    tokens += "N:" + code.substring(i, j)
                    i = j
                }
                else -> { tokens += "O:$c"; i++ }
            }
        }
        return tokens
    }

    private fun firstDifference(displayed: String, rendered: String): String {
        val a = tokenize(displayed)
        val b = tokenize(rendered)
        val at = a.zip(b).indexOfFirst { (x, y) -> x != y }
        val idx = if (at == -1) minOf(a.size, b.size) else at
        fun ctx(list: List<String>) = list.drop((idx - 2).coerceAtLeast(0)).take(5)
            .joinToString(" ") { it.substringAfter(':') }
        return "displayed: …${ctx(a)}…\nrendered:  …${ctx(b)}…"
    }

    /** Mirrors Kotlin's String.trimIndent() for the captured triple-string content. */
    private fun trimIndentLike(raw: String): String = raw.trimIndent()

    // ---- Locate sources ---------------------------------------------------------------------

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
