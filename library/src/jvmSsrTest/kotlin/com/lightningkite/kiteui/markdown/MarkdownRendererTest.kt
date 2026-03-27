// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.ssr.SsrDocument
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.col
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MarkdownRendererTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun <T> runWithTimeout(timeoutMs: Long = 5000, block: () -> T): T = runBlocking {
        withTimeout(timeoutMs) { block() }
    }

    @Test
    fun testBasicHeading() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("# Hello World")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Hello World"), "HTML should contain heading text")
    }

    @Test
    fun testMultipleHeadings() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                # Heading 1
                ## Heading 2
                ### Heading 3
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Heading 1"), "HTML should contain H1 text")
        assertTrue(result.html.contains("Heading 2"), "HTML should contain H2 text")
        assertTrue(result.html.contains("Heading 3"), "HTML should contain H3 text")
    }

    @Test
    fun testBoldAndItalic() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("This is **bold** and *italic* text.")
        }
        val result = context.serialize()

        // Note: microparse adds space after tag name, so <b> becomes <b >
        assertTrue(result.html.contains("<b") && result.html.contains("</b>"), "HTML should contain bold tag")
        assertTrue(result.html.contains("<i") && result.html.contains("</i>"), "HTML should contain italic tag")
        assertTrue(result.html.contains("bold"), "HTML should contain bold text")
        assertTrue(result.html.contains("italic"), "HTML should contain italic text")
    }

    @Test
    fun testInlineCode() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("Use `code` here")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("<tt") && result.html.contains("</tt>"), "HTML should contain tt tag")
        assertTrue(result.html.contains("code"), "HTML should contain code text")
    }

    @Test
    fun testLink() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("[Click here](https://example.com)")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("href=\"https://example.com\""), "HTML should contain link href")
        assertTrue(result.html.contains("Click here"), "HTML should contain link text")
    }

    @Test
    fun testUnorderedList() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                - Item 1
                - Item 2
                - Item 3
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Item 1"), "HTML should contain list item 1")
        assertTrue(result.html.contains("Item 2"), "HTML should contain list item 2")
        assertTrue(result.html.contains("Item 3"), "HTML should contain list item 3")
        // Should contain bullet markers
        assertTrue(result.html.contains("\u2022") || result.html.contains("•"), "HTML should contain bullet markers")
    }

    @Test
    fun testNestedList() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                - Item 1
                  - Sub item
                - Item 2
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Item 1"), "HTML should contain Item 1")
        assertTrue(result.html.contains("Sub item"), "HTML should contain Sub item")
        assertTrue(result.html.contains("Item 2"), "HTML should contain Item 2")
        // Both levels should have bullet markers
        assertTrue(result.html.count { it == '•' } >= 3, "HTML should have bullets for all items including nested")
    }

    @Test
    fun testOrderedList() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                1. First
                2. Second
                3. Third
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("First"), "HTML should contain first item")
        assertTrue(result.html.contains("1."), "HTML should contain number marker")
    }

    @Test
    fun testBlockquote() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("> This is a quote")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("This is a quote"), "HTML should contain quote text")
    }

    @Test
    fun testCodeBlock() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                ```kotlin
                fun main() {
                    println("Hello")
                }
                ```
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("fun main()"), "HTML should contain code")
        assertTrue(result.html.contains("println"), "HTML should contain println")
    }

    @Test
    fun testHorizontalRule() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                Above

                ---

                Below
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Above"), "HTML should contain text above rule")
        assertTrue(result.html.contains("Below"), "HTML should contain text below rule")
    }

    @Test
    fun testTable() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("""
                | Name | Age |
                |------|-----|
                | John | 30  |
                | Jane | 25  |
            """.trimIndent())
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Name"), "HTML should contain header Name")
        assertTrue(result.html.contains("Age"), "HTML should contain header Age")
        assertTrue(result.html.contains("John"), "HTML should contain John")
        assertTrue(result.html.contains("30"), "HTML should contain 30")
        assertTrue(result.html.contains("Jane"), "HTML should contain Jane")
    }

    @Test
    fun testBlockLink() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("[> # Linked Heading](https://example.com)")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Linked Heading"), "HTML should contain heading text")
        // Note: href may use single quotes in SSR output
        assertTrue(result.html.contains("href") && result.html.contains("https://example.com"), "HTML should contain link href")
    }

    @Test
    fun testInternalLinks() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown(
                "[Go Home](/home)",
                config = MarkdownConfig.withInternalLinks(
                    pattern = Regex("^/.*"),
                    resolver = { null } // We can't easily create pages in SSR test
                )
            )
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Go Home"), "HTML should contain link text")
        // Without a resolved page, it falls back to external link
        assertTrue(result.html.contains("href=\"/home\""), "HTML should contain href")
    }

    @Test
    fun testImageBasic() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("![Alt text](image.png)")
        }
        val result = context.serialize()

        // Images are now rendered as actual img elements
        assertTrue(result.html.contains("<img"), "HTML should contain img tag")
        assertTrue(result.html.contains("image.png"), "HTML should contain image src")
    }

    @Test
    fun testImageWithSize() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("![Icon|32](icon.png)")
        }
        val result = context.serialize()

        // Images with size constraints should be rendered
        assertTrue(result.html.contains("<img"), "HTML should contain img tag")
        assertTrue(result.html.contains("icon.png"), "HTML should contain image src")
    }

    @Test
    fun testComplexDocument() = runWithTimeout {
        val context = SsrContext("/")
        context.title = "Markdown Test"

        context.render {
            markdown("""
                # Welcome

                This is a **test** document with *various* features.

                ## Features

                - Lists
                - **Bold** items
                - [Links](https://example.com)

                > A blockquote with some quoted text

                ```kotlin
                println("Code blocks work!")
                ```

                ---

                | Feature | Status |
                |---------|--------|
                | Bold    | Yes    |
                | Italic  | Yes    |

                The end.
            """.trimIndent())
        }
        val result = context.serialize()

        // Verify various elements (microparse adds space after tag name)
        assertTrue(result.html.contains("Welcome"), "HTML should contain Welcome heading")
        assertTrue(result.html.contains("<b") && result.html.contains("test"), "HTML should contain bold text")
        assertTrue(result.html.contains("<i") && result.html.contains("various"), "HTML should contain italic text")
        assertTrue(result.html.contains("Lists"), "HTML should contain list items")
        assertTrue(result.html.contains("blockquote"), "HTML should contain blockquote text")
        assertTrue(result.html.contains("println"), "HTML should contain code")
        assertTrue(result.html.contains("Feature"), "HTML should contain table header")
        assertTrue(result.html.contains("The end"), "HTML should contain final paragraph")

        // Save to file for visual inspection
        val document = SsrDocument(baseHref = "/")
        val html = document.render(result)
        File("build/test-markdown-renderer.html").writeText(html)
    }

    @Test
    fun testCustomBlock() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown(
                """
                ::: note
                This is a note
                :::
                """.trimIndent(),
                config = MarkdownConfig().withCustomBlocks(
                    mapOf(
                        "note" to object : CustomBlockHandler {
                            override fun ViewWriter.render(
                                children: List<MarkdownNode>,
                                attributes: Map<String, String>,
                                context: MarkdownRenderContext
                            ) {
                                card.col {
                                    with(context) { children.forEach { renderNode(it) } }
                                }
                            }
                        }
                    )
                )
            )
        }
        val result = context.serialize()

        assertTrue(result.html.contains("This is a note"), "HTML should contain note content")
    }

    @Test
    fun testCustomBlockWithHref() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown(
                """
                ::: card href="/some/path"
                Click this card!
                :::
                """.trimIndent()
            )
        }
        val result = context.serialize()

        println("=== CustomBlock with href HTML ===")
        println(result.html)
        println("=== End HTML ===")

        assertTrue(result.html.contains("Click this card"), "HTML should contain card content")
        // Check for anchor tag with href
        assertTrue(result.html.contains("<a"), "HTML should contain anchor tag")
        assertTrue(result.html.contains("/some/path"), "HTML should contain link path")
    }

    @Test
    fun testCustomBlockWithHttpsHref() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown(
                """
                ::: card href="https://example.com"
                This entire card is a **clickable link**!
                :::
                """.trimIndent()
            )
        }
        val result = context.serialize()

        println("=== CustomBlock with HTTPS href HTML ===")
        println(result.html)
        println("=== End HTML ===")

        assertTrue(result.html.contains("clickable link"), "HTML should contain card content")
        // Check for anchor tag with href attribute
        assertTrue(result.html.contains("<a") && result.html.contains("href"), "HTML should contain anchor tag with href")
        assertTrue(result.html.contains("https://example.com"), "HTML should contain the URL")
    }

    @Test
    fun testEscapedHtml() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("This contains <script>alert('xss')</script> which should be escaped")
        }
        val result = context.serialize()

        // HTML special characters should be escaped
        assertTrue(result.html.contains("&lt;script&gt;") || !result.html.contains("<script>"),
            "Script tags should be escaped or not present")
    }

    @Test
    fun testStrikethrough() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("This is ~~deleted~~ text")
        }
        val result = context.serialize()

        assertTrue(result.html.contains("<s") && result.html.contains("</s>"), "HTML should contain strikethrough tag")
        assertTrue(result.html.contains("deleted"), "HTML should contain deleted text")
    }

    @Test
    fun testNestedFormatting() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("This is ***bold and italic*** text")
        }
        val result = context.serialize()

        // Should have both bold and italic (microparse adds space after tag name)
        assertTrue(result.html.contains("<b") && result.html.contains("</b>"), "HTML should contain bold tag")
        assertTrue(result.html.contains("<i") && result.html.contains("</i>"), "HTML should contain italic tag")
    }

    @Test
    fun testEmptyMarkdown() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown("")
        }
        val result = context.serialize()

        // Should not crash and should produce valid HTML
        assertTrue(result.html.contains("<") && result.html.contains(">"), "HTML should have tags")
    }

    // by Claude - Test custom block handler with parse context
    @Test
    fun testCustomBlockWithParseContext() = runWithTimeout {
        val context = SsrContext("/")
        context.render {
            markdown(
                """
                ::: split
                Left side content with **bold**
                ---
                Right side content with *italic*
                :::
                """.trimIndent(),
                config = MarkdownConfig().withCustomBlocks(
                    mapOf(
                        "split" to object : CustomBlockHandler {
                            // Use parse to split content at --- and parse each part as markdown
                            override fun parse(
                                blockType: String,
                                attributes: Map<String, String>,
                                content: String,
                                context: MarkdownParseContext
                            ): MarkdownNode {
                                val parts = content.split(Regex("\\n---\\n"), limit = 2)
                                if (parts.size != 2) {
                                    // Fall back to default parsing
                                    return MarkdownNode.CustomBlock(blockType, attributes, context.parseBlocks(content))
                                }

                                // Parse each part as markdown blocks using the context
                                val leftChildren = context.parseBlocks(parts[0])
                                val rightChildren = context.parseBlocks(parts[1])

                                // Return a custom node structure that wraps both sides
                                return MarkdownNode.CustomBlock(
                                    type = blockType,
                                    attributes = attributes + ("sides" to "2"),
                                    children = leftChildren + rightChildren
                                )
                            }

                            override fun ViewWriter.render(
                                children: List<MarkdownNode>,
                                attributes: Map<String, String>,
                                context: MarkdownRenderContext
                            ) {
                                col {
                                    with(context) { children.forEach { renderNode(it) } }
                                }
                            }
                        }
                    )
                )
            )
        }
        val result = context.serialize()

        assertTrue(result.html.contains("Left side content"), "HTML should contain left side content")
        assertTrue(result.html.contains("Right side content"), "HTML should contain right side content")
        // Both parts should be parsed as markdown
        assertTrue(result.html.contains("<b") && result.html.contains("bold"), "HTML should contain bold from left side")
        assertTrue(result.html.contains("<i") && result.html.contains("italic"), "HTML should contain italic from right side")
    }
}
