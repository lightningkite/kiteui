package com.lightningkite.kiteui

import com.lightningkite.kiteui.dom.parseMinimalHtmlNodes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Security properties of the [parseMinimalHtmlNodes] / secure() sanitizer.
 *
 * This sanitizer is the only thing standing between caller-supplied HTML and the DOM:
 * TextView.setBasicHtmlContent (public API in commonMain) pipes its argument through
 * secure() straight into innerHtmlUnsafe on web. Any string that survives secure()
 * carrying script-executing markup is an XSS vector in every KiteUI web app that renders
 * user-generated content through that API.
 *
 * The pre-existing MicroparseTest.secure() case only prints its output and asserts
 * nothing, which is why these holes went unnoticed.
 */
class MicroparseSecurityTest {

    private fun sanitize(html: String): String =
        html.parseMinimalHtmlNodes().onEach { it.secure() }.joinToString(" ")

    @Test
    fun scriptTagsAreNeutralized() {
        val out = sanitize("<script>alert(1)</script>")
        assertFalse(out.contains("<script"), "script tag survived sanitization: $out")
    }

    @Test
    fun eventHandlerAttributesAreStripped() {
        val out = sanitize("""<button onclick="alert(1)">x</button>""")
        assertFalse(out.contains("onclick"), "event handler survived sanitization: $out")
    }

    @Test
    fun javascriptUrlSchemeIsRejected() {
        // `a` is an allowed tag and `href` an allowed attribute, but the attribute VALUE is
        // never inspected, so a javascript: URL passes through untouched.
        val out = sanitize("""<a href="javascript:alert(1)">click</a>""")
        assertFalse(
            out.lowercase().contains("javascript:"),
            "javascript: URL survived sanitization: $out",
        )
    }

    @Test
    fun dataUrlSchemeIsRejected() {
        val out = sanitize("""<a href="data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==">x</a>""")
        assertFalse(
            out.lowercase().contains("data:text/html"),
            "data: HTML URL survived sanitization: $out",
        )
    }

    @Test
    fun attributeValuesAreEscapedOnOutput() {
        // Serialization wraps values in double quotes without escaping them, so a value
        // containing a double quote closes the attribute early and everything after it is
        // re-parsed by the browser as new attributes. That bypasses the okAttrs allow-list
        // entirely, because the injected attribute never passed through it.
        val out = sanitize("""<a href='x" onmouseover="alert(1)'>y</a>""")
        assertFalse(
            out.contains("onmouseover=\"alert(1)\""),
            "attribute injection via unescaped quote succeeded: $out",
        )
    }

    @Test
    fun allowedMarkupIsPreserved() {
        // Guards against a fix that over-corrects and strips legitimate content.
        val out = sanitize("<p>Hello <strong>world</strong></p>")
        assertTrue(out.contains("<p"), "paragraph tag was dropped: $out")
        assertTrue(out.contains("<strong"), "strong tag was dropped: $out")
        assertTrue(out.contains("Hello"), "text content was dropped: $out")
    }

    @Test
    fun relativeAndHttpLinksArePreserved() {
        val http = sanitize("""<a href="https://example.com">x</a>""")
        assertTrue(http.contains("https://example.com"), "https link was dropped: $http")
        val rel = sanitize("""<a href="/about">x</a>""")
        assertTrue(rel.contains("/about"), "relative link was dropped: $rel")
    }

    @Test
    fun uppercaseMarkupIsRecognizedRatherThanMangled() {
        // HTML tag and attribute names are case-insensitive; the allow-lists are lowercase. Matching
        // them as-written failed closed, which is safe but turned every `<A HREF>` into a bare span
        // and silently threw the link away.
        val out = sanitize("""<A HREF="https://example.com">x</A>""")
        assertTrue(out.contains("<a "), "uppercase anchor was not recognized: $out")
        assertTrue(out.contains("https://example.com"), "uppercase href was dropped: $out")
    }

    @Test
    fun uppercaseUnsafeSchemesAreStillRejected() {
        // The flip side of normalizing case: it must not open a way in.
        val out = sanitize("""<A HREF="JavaScript:alert(1)">x</A>""")
        assertFalse(out.lowercase().contains("javascript:"), "javascript: URL survived: $out")
    }

    @Test
    fun deeplyNestedMarkupDoesNotOverflowTheStack() {
        // secure() and toString() both recurse per level, so unbounded nesting was a stack overflow
        // reachable from any user-generated content rendered through setBasicHtmlContent.
        val depth = 50_000
        val out = sanitize("<div>".repeat(depth) + "boom" + "</div>".repeat(depth))
        assertTrue(out.contains("boom"), "content was lost entirely: ${out.take(80)}")
    }
}
