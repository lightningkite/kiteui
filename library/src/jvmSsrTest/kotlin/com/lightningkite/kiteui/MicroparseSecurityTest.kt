package com.lightningkite.kiteui

import com.lightningkite.kiteui.dom.parseMPNodes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Security properties of the [parseMPNodes] / secure() sanitizer.
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
        html.parseMPNodes().onEach { it.secure() }.joinToString(" ")

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
}
