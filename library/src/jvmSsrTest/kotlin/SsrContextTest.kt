package com.lightningkite.kiteui.dom

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.ssr.SsrDocument
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SsrContextTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun testBasicRendering() {
        val context = SsrContext("/")
        context.title = "Test Page"
        context.description = "A test page for SSR"

        val result = context.renderAndSerialize {  // by Claude - use renderAndSerialize to get SsrResult
            col {
                h1("Hello SSR!")
                text("This is server-side rendered content.")
                button {
                    text("Click me")
                }
            }
        }

        // Verify HTML contains expected content
        assertTrue(result.html.contains("Hello SSR!"), "HTML should contain heading text")
        assertTrue(result.html.contains("Click me"), "HTML should contain button text")
        // Note: h1() creates a styled text element, not an actual <h1> tag
        assertTrue(result.html.contains("<") && result.html.contains(">"), "HTML should have tags")

        // Verify metadata
        assertTrue(result.title == "Test Page", "Title should be set")
        assertTrue(result.description == "A test page for SSR", "Description should be set")

        // Verify we got some CSS
        assertTrue(result.css.isNotEmpty() || result.headElements.isNotEmpty(), "Should have CSS or head elements")
    }

    @Test
    fun testThemedRendering() {
        val context = SsrContext("/")
        context.title = "Themed Page"

        val result = context.renderAndSerialize {  // by Claude - use renderAndSerialize to get SsrResult
            Theme.flat2("test", hue = 0.6.turns).onNext.scrolling.col {
                sizeConstraints(width = 50.rem).card.col {
                    h1("Welcome to my Website")
                    text("I hope you like it!")
                }
                sizeConstraints(width = 50.rem).card.col {
                    h2("Form")
                    field("Email") {
                        textInput { }
                    }
                    field("Phone Number") {
                        textInput { }
                    }
                    important.button { text("Submit") }
                }
            }
        }

        // Verify HTML
        assertTrue(result.html.contains("Welcome to my Website"), "HTML should contain heading")
        assertTrue(result.html.contains("Submit"), "HTML should contain button text")
        assertTrue(result.html.contains("<input"), "HTML should have input elements")

        // Verify CSS was generated (themes generate CSS rules)
        assertTrue(result.css.isNotEmpty(), "Should have generated CSS from theme")
    }

    @Test
    fun testDocumentRendering() {
        val context = SsrContext("/")
        context.title = "Full Document Test"
        context.description = "Testing complete HTML document rendering"
        context.metaTags["og:title"] = "Full Document Test"
        context.metaTags["og:type"] = "website"

        val result = context.renderAndSerialize {  // by Claude - use renderAndSerialize to get SsrResult
            col {
                h1("Hello World")
                text("Content goes here")
            }
        }

        val document = SsrDocument(baseHref = "/")
        val html = document.render(result)

        // Verify document structure
        assertTrue(html.contains("<!DOCTYPE html>"), "Should have DOCTYPE")
        assertTrue(html.contains("<html lang=\"en\">"), "Should have html tag with lang")
        assertTrue(html.contains("<title>Full Document Test</title>"), "Should have title tag")
        assertTrue(html.contains("name=\"description\""), "Should have meta description")
        assertTrue(html.contains("og:title"), "Should have OpenGraph title")
        assertTrue(html.contains("og:type"), "Should have OpenGraph type")
        assertTrue(html.contains("<base href=\"/\">"), "Should have base href")
        assertTrue(html.contains("Hello World"), "Should contain body content")

        // Save to file for visual inspection
        File("build/test-ssr-context.html").writeText(html)
    }

    @Test
    fun testPreloadedData() {
        val context = SsrContext("/")

        // Preload some data
        context.preload("user", mapOf("name" to "John", "email" to "john@example.com"))
        context.preload("count", 42)

        // Verify preloaded data can be retrieved
        val user = context.getPreloaded<Map<String, String>>("user")
        assertTrue(user != null, "User should be retrievable")
        assertTrue(user["name"] == "John", "User name should match")

        val count = context.getPreloaded<Int>("count")
        assertTrue(count == 42, "Count should match")

        val missing = context.getPreloaded<String>("nonexistent")
        assertTrue(missing == null, "Missing key should return null")
    }

    @Test
    fun testRequestIsolation() {
        // Create two separate contexts
        val context1 = SsrContext("/")
        context1.title = "Page 1"
        context1.preload("data", "context1-data")

        val context2 = SsrContext("/")
        context2.title = "Page 2"
        context2.preload("data", "context2-data")

        // Render in both contexts
        // by Claude - use renderAndSerialize to get SsrResult
        val result1 = context1.renderAndSerialize {
            text("Content 1")
        }

        val result2 = context2.renderAndSerialize {
            text("Content 2")
        }

        // Verify they're isolated
        assertTrue(result1.title == "Page 1", "Context 1 title should be Page 1")
        assertTrue(result2.title == "Page 2", "Context 2 title should be Page 2")
        assertTrue(context1.getPreloaded<String>("data") == "context1-data", "Context 1 data should be isolated")
        assertTrue(context2.getPreloaded<String>("data") == "context2-data", "Context 2 data should be isolated")
        assertTrue(result1.html.contains("Content 1"), "Result 1 should have Content 1")
        assertTrue(result2.html.contains("Content 2"), "Result 2 should have Content 2")
    }

    @Test
    fun testVoidElementsRenderCorrectly() {
        val context = SsrContext("/")

        val result = context.renderAndSerialize {  // by Claude - use renderAndSerialize to get SsrResult
            col {
                textInput { }  // Should render as <input ... />
                text("Line 1")
                // br would render as <br/> if we had it
                text("Line 2")
            }
        }

        // Input should self-close
        assertTrue(result.html.contains("<input") && result.html.contains("/>"), "Input should self-close")

        // Divs should not self-close (kiteui-col is a div)
        assertTrue(result.html.contains("</div>"), "Divs should have closing tags")
    }
}
