// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.px
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownParserTest {
    private val parser = MarkdownParser()

    // ================================
    // Heading tests
    // ================================

    @Test
    fun testHeadingH1() {
        val doc = parser.parse("# Hello World")
        assertEquals(1, doc.children.size)
        val heading = doc.children.single() as MarkdownNode.Heading
        assertEquals(1, heading.level)
        assertEquals("Hello World", (heading.content.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testHeadingH2() {
        val doc = parser.parse("## Subtitle")
        val heading = doc.children.single() as MarkdownNode.Heading
        assertEquals(2, heading.level)
        assertEquals("Subtitle", (heading.content.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testHeadingH6() {
        val doc = parser.parse("###### Smallest")
        val heading = doc.children.single() as MarkdownNode.Heading
        assertEquals(6, heading.level)
    }

    @Test
    fun testHeadingWithFormatting() {
        val doc = parser.parse("# Hello **World**")
        val heading = doc.children.single() as MarkdownNode.Heading
        assertEquals(2, heading.content.size)
        assertTrue(heading.content[0] is MarkdownNode.Text)
        assertTrue(heading.content[1] is MarkdownNode.Bold)
    }

    // ================================
    // Paragraph tests
    // ================================

    @Test
    fun testSimpleParagraph() {
        val doc = parser.parse("This is a paragraph.")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertEquals("This is a paragraph.", (para.content.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testMultiLineParagraph() {
        val doc = parser.parse("Line one\nLine two")
        val para = doc.children.single() as MarkdownNode.Paragraph
        // Multi-line paragraphs are joined with space
        assertEquals("Line one Line two", (para.content.single() as MarkdownNode.Text).content)
    }

    // ================================
    // Bold and Italic tests
    // ================================

    @Test
    fun testBoldAsterisks() {
        val doc = parser.parse("This is **bold** text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertEquals(3, para.content.size)
        assertTrue(para.content[1] is MarkdownNode.Bold)
        val bold = para.content[1] as MarkdownNode.Bold
        assertEquals("bold", (bold.children.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testBoldUnderscores() {
        val doc = parser.parse("This is __bold__ text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.any { it is MarkdownNode.Bold })
    }

    @Test
    fun testItalicAsterisks() {
        val doc = parser.parse("This is *italic* text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.any { it is MarkdownNode.Italic })
    }

    @Test
    fun testItalicUnderscores() {
        val doc = parser.parse("This is _italic_ text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.any { it is MarkdownNode.Italic })
    }

    @Test
    fun testBoldItalic() {
        val doc = parser.parse("This is ***bold italic*** text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        // Should be Bold containing Italic
        val boldNode = para.content.find { it is MarkdownNode.Bold } as MarkdownNode.Bold
        assertTrue(boldNode.children.single() is MarkdownNode.Italic)
    }

    @Test
    fun testStrikethrough() {
        val doc = parser.parse("This is ~~deleted~~ text")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.any { it is MarkdownNode.Strikethrough })
    }

    // ================================
    // Inline code tests
    // ================================

    @Test
    fun testInlineCode() {
        val doc = parser.parse("Use `code` here")
        val para = doc.children.single() as MarkdownNode.Paragraph
        val code = para.content.find { it is MarkdownNode.InlineCode } as MarkdownNode.InlineCode
        assertEquals("code", code.content)
    }

    @Test
    fun testInlineCodeWithBackticks() {
        val doc = parser.parse("Use ``code with `backticks``` here")
        val para = doc.children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.any { it is MarkdownNode.InlineCode })
    }

    // ================================
    // Link tests
    // ================================

    @Test
    fun testLink() {
        val doc = parser.parse("Click [here](https://example.com) for more")
        val para = doc.children.single() as MarkdownNode.Paragraph
        val link = para.content.find { it is MarkdownNode.Link } as MarkdownNode.Link
        assertEquals("https://example.com", link.url)
        assertEquals("here", (link.children.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testLinkWithFormattedText() {
        val doc = parser.parse("[**Bold Link**](url)")
        val para = doc.children.single() as MarkdownNode.Paragraph
        val link = para.content.single() as MarkdownNode.Link
        assertTrue(link.children.single() is MarkdownNode.Bold)
    }

    // ================================
    // Image tests
    // ================================

    @Test
    fun testImage() {
        val doc = parser.parse("![alt text](image.png)")
        val para = doc.children.single() as MarkdownNode.Paragraph
        val image = para.content.single() as MarkdownNode.Image
        assertEquals("image.png", image.url)
        assertEquals("alt text", image.alt)
        assertEquals(null, image.size)
    }

    @Test
    fun testImageWithSize() {
        val doc = parser.parse("![alt|32](image.png)")
        val para = doc.children.single() as MarkdownNode.Paragraph
        val image = para.content.single() as MarkdownNode.Image
        assertEquals("image.png", image.url)
        assertEquals("alt", image.alt)
        assertEquals(32.px, image.size)
    }

    // ================================
    // Code block tests
    // ================================

    @Test
    fun testCodeBlock() {
        val doc = parser.parse("""
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent())
        val codeBlock = doc.children.single() as MarkdownNode.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("fun main()"))
    }

    @Test
    fun testCodeBlockNoLanguage() {
        val doc = parser.parse("""
            ```
            some code
            ```
        """.trimIndent())
        val codeBlock = doc.children.single() as MarkdownNode.CodeBlock
        assertEquals(null, codeBlock.language)
        assertEquals("some code", codeBlock.code)
    }

    // ================================
    // Blockquote tests
    // ================================

    @Test
    fun testBlockquote() {
        val doc = parser.parse("> This is a quote")
        val quote = doc.children.single() as MarkdownNode.Blockquote
        val para = quote.children.single() as MarkdownNode.Paragraph
        assertEquals("This is a quote", (para.content.single() as MarkdownNode.Text).content)
    }

    @Test
    fun testNestedBlockquote() {
        val doc = parser.parse("""
            > Outer quote
            > > Inner quote
        """.trimIndent())
        val quote = doc.children.single() as MarkdownNode.Blockquote
        assertTrue(quote.children.any { it is MarkdownNode.Blockquote })
    }

    // ================================
    // List tests
    // ================================

    @Test
    fun testUnorderedList() {
        val doc = parser.parse("""
            - Item 1
            - Item 2
            - Item 3
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.UnorderedList
        assertEquals(3, list.items.size)
    }

    @Test
    fun testUnorderedListAsterisk() {
        val doc = parser.parse("""
            * Item 1
            * Item 2
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.UnorderedList
        assertEquals(2, list.items.size)
    }

    @Test
    fun testOrderedList() {
        val doc = parser.parse("""
            1. First
            2. Second
            3. Third
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.OrderedList
        assertEquals(3, list.items.size)
        assertEquals(1, list.startNumber)
    }

    @Test
    fun testOrderedListCustomStart() {
        val doc = parser.parse("""
            5. Fifth
            6. Sixth
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.OrderedList
        assertEquals(2, list.items.size)
        assertEquals(5, list.startNumber)
    }

    @Test
    fun testNestedUnorderedList() {
        val doc = parser.parse("""
            - Item 1
              - Sub item
            - Item 2
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.UnorderedList
        assertEquals(2, list.items.size)
        val firstItem = list.items[0] as MarkdownNode.ListItem
        assertTrue(firstItem.children.any { it is MarkdownNode.UnorderedList })
        val nestedList = firstItem.children.filterIsInstance<MarkdownNode.UnorderedList>().single()
        assertEquals(1, nestedList.items.size)
        val secondItem = list.items[1] as MarkdownNode.ListItem
        assertEquals(1, secondItem.children.size)
    }

    @Test
    fun testNestedUnorderedListMultipleSubs() {
        val doc = parser.parse("""
            - Item 1
              - Sub 1
              - Sub 2
            - Item 2
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.UnorderedList
        assertEquals(2, list.items.size)
        val nestedList = (list.items[0] as MarkdownNode.ListItem).children.filterIsInstance<MarkdownNode.UnorderedList>().single()
        assertEquals(2, nestedList.items.size)
    }

    @Test
    fun testNestedOrderedList() {
        val doc = parser.parse("""
            1. First
              1. First sub
              2. Second sub
            2. Second
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.OrderedList
        assertEquals(2, list.items.size)
        val firstItem = list.items[0]
        assertTrue(firstItem.children.any { it is MarkdownNode.OrderedList })
        val nestedList = firstItem.children.filterIsInstance<MarkdownNode.OrderedList>().single()
        assertEquals(2, nestedList.items.size)
    }

    @Test
    fun testListItemBoldStart() {
        val doc = parser.parse("- **Bold** text item")
        val list = doc.children.single() as MarkdownNode.UnorderedList
        val para = (list.items.single() as MarkdownNode.ListItem).children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.first() is MarkdownNode.Bold)
    }

    @Test
    fun testListItemItalicStart() {
        val doc = parser.parse("- *italic* text item")
        val list = doc.children.single() as MarkdownNode.UnorderedList
        val para = (list.items.single() as MarkdownNode.ListItem).children.single() as MarkdownNode.Paragraph
        assertTrue(para.content.first() is MarkdownNode.Italic)
    }

    @Test
    fun testTaskList() {
        val doc = parser.parse("""
            - [ ] Unchecked
            - [x] Checked
            - [X] Also checked
        """.trimIndent())
        val list = doc.children.single() as MarkdownNode.UnorderedList
        assertEquals(3, list.items.size)
        // Verify they're TaskListItems with correct checked state
        assertTrue(list.items.all { it is MarkdownNode.TaskListItem })
        val taskItems = list.items.filterIsInstance<MarkdownNode.TaskListItem>()
        assertEquals(false, taskItems[0].checked)
        assertEquals(true, taskItems[1].checked)
        assertEquals(true, taskItems[2].checked)
    }

    // ================================
    // Horizontal rule tests
    // ================================

    @Test
    fun testHorizontalRuleDashes() {
        val doc = parser.parse("---")
        assertTrue(doc.children.single() is MarkdownNode.HorizontalRule)
    }

    @Test
    fun testHorizontalRuleAsterisks() {
        val doc = parser.parse("***")
        assertTrue(doc.children.single() is MarkdownNode.HorizontalRule)
    }

    @Test
    fun testHorizontalRuleUnderscores() {
        val doc = parser.parse("___")
        assertTrue(doc.children.single() is MarkdownNode.HorizontalRule)
    }

    // ================================
    // Table tests
    // ================================

    @Test
    fun testTable() {
        val doc = parser.parse("""
            | Col1 | Col2 |
            |------|------|
            | A    | B    |
            | C    | D    |
        """.trimIndent())
        val table = doc.children.single() as MarkdownNode.Table
        assertEquals(2, table.headers.size)
        assertEquals(2, table.rows.size)
    }

    @Test
    fun testTableAlignments() {
        val doc = parser.parse("""
            | Left | Center | Right |
            |:-----|:------:|------:|
            | A    | B      | C     |
        """.trimIndent())
        val table = doc.children.single() as MarkdownNode.Table
        assertEquals(Align.Start, table.alignments[0])
        assertEquals(Align.Center, table.alignments[1])
        assertEquals(Align.End, table.alignments[2])
    }

    // ================================
    // Block link tests
    // ================================

    @Test
    fun testBlockLinkSingleLine() {
        val doc = parser.parse("[> # Linked Heading](https://example.com)")
        val blockLink = doc.children.single() as MarkdownNode.BlockLink
        assertEquals("https://example.com", blockLink.url)
        assertTrue(blockLink.children.any { it is MarkdownNode.Heading })
    }

    @Test
    fun testBlockLinkMultiLine() {
        val doc = parser.parse("""
            [>
            # Card Title
            Some description text
            ](https://example.com)
        """.trimIndent())
        val blockLink = doc.children.single() as MarkdownNode.BlockLink
        assertEquals("https://example.com", blockLink.url)
        assertEquals(2, blockLink.children.size)
    }

    // ================================
    // Custom block tests
    // ================================

    @Test
    fun testCustomBlock() {
        val doc = parser.parse("""
            ::: note
            This is a note
            :::
        """.trimIndent())
        val customBlock = doc.children.single() as MarkdownNode.CustomBlock
        assertEquals("note", customBlock.type)
        assertTrue(customBlock.attributes.isEmpty())
        assertTrue(customBlock.children.any { it is MarkdownNode.Paragraph })
    }

    @Test
    fun testCustomBlockWithHref() {
        val doc = parser.parse("""
            ::: card href="/path/to/page"
            Click me!
            :::
        """.trimIndent())
        val customBlock = doc.children.single() as MarkdownNode.CustomBlock
        assertEquals("card", customBlock.type)
        println("Attributes: ${customBlock.attributes}")
        assertEquals("/path/to/page", customBlock.attributes["href"])
        assertTrue(customBlock.children.any { it is MarkdownNode.Paragraph })
    }

    @Test
    fun testCustomBlockWithMultipleAttributes() {
        val doc = parser.parse("""
            ::: card href="/page" class="featured" id="main"
            Content here
            :::
        """.trimIndent())
        val customBlock = doc.children.single() as MarkdownNode.CustomBlock
        assertEquals("card", customBlock.type)
        assertEquals("/page", customBlock.attributes["href"])
        assertEquals("featured", customBlock.attributes["class"])
        assertEquals("main", customBlock.attributes["id"])
    }

    // ================================
    // Complex document tests
    // ================================

    @Test
    fun testComplexDocument() {
        val doc = parser.parse("""
            # Welcome

            This is a **test** document with *various* features.

            ## Features

            - Lists
            - **Bold** items
            - [Links](url)

            > A blockquote

            ```kotlin
            println("Code")
            ```

            ---

            The end.
        """.trimIndent())

        // Should have multiple blocks
        assertTrue(doc.children.size >= 7)

        // First is heading
        assertTrue(doc.children[0] is MarkdownNode.Heading)

        // Should have various block types
        assertTrue(doc.children.any { it is MarkdownNode.Paragraph })
        assertTrue(doc.children.any { it is MarkdownNode.UnorderedList })
        assertTrue(doc.children.any { it is MarkdownNode.Blockquote })
        assertTrue(doc.children.any { it is MarkdownNode.CodeBlock })
        assertTrue(doc.children.any { it is MarkdownNode.HorizontalRule })
    }

    // ================================
    // Edge case tests
    // ================================

    @Test
    fun testEmptyInput() {
        val doc = parser.parse("")
        assertTrue(doc.children.isEmpty())
    }

    @Test
    fun testOnlyWhitespace() {
        val doc = parser.parse("   \n\n   ")
        assertTrue(doc.children.isEmpty())
    }

    @Test
    fun testEscapedCharacters() {
        val doc = parser.parse("This is \\*not italic\\*")
        val para = doc.children.single() as MarkdownNode.Paragraph
        // Should not contain italic node, just text with asterisks
        assertTrue(para.content.none { it is MarkdownNode.Italic })
    }

    @Test
    fun testUnmatchedBold() {
        // Unmatched ** should be treated as text
        val doc = parser.parse("This is **not closed")
        val para = doc.children.single() as MarkdownNode.Paragraph
        // Should not crash and should have some text content
        assertTrue(para.content.isNotEmpty())
    }
}
