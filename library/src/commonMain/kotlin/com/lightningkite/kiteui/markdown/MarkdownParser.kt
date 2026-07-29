// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.px

/**
 * Custom markdown parser that converts markdown text into an AST.
 * Supports standard markdown features plus custom extensions:
 * - Sized images: ![alt|32](url) → size = 32.px
 * - Custom blocks: ::: type ... :::
 * - Block links: [> block content](url)
 *
 * @param customBlocks Optional handlers for custom block types
 */
public class MarkdownParser(
    private val customBlocks: Map<String, CustomBlockHandler> = emptyMap()
) : MarkdownParseContext {

    // by Claude - MarkdownParseContext implementation
    override fun parseBlocks(content: String): List<MarkdownNode> {
        return parseBlocksInternal(content.lines())
    }
    /**
     * Parse markdown text into a Document AST.
     */
    public fun parse(markdown: String): MarkdownNode.Document {
        val lines = markdown.lines()
        val blocks = parseBlocksInternal(lines)
        return MarkdownNode.Document(blocks)
    }

    // ================================
    // Block-level parsing
    // ================================

    private fun parseBlocksInternal(lines: List<String>): List<MarkdownNode> {
        val result = mutableListOf<MarkdownNode>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                // Empty line - skip
                trimmed.isEmpty() -> {
                    i++
                }

                // Block link: [> or multi-line
                trimmed.startsWith("[>") -> {
                    val (blockLink, consumed) = parseBlockLink(lines, i)
                    if (blockLink != null) {
                        result.add(blockLink)
                        i += consumed
                    } else {
                        // Fall back to paragraph
                        val (para, consumed2) = parseParagraph(lines, i)
                        result.add(para)
                        i += consumed2
                    }
                }

                // Heading: # ## ### etc
                trimmed.startsWith("#") -> {
                    val heading = parseHeading(trimmed)
                    if (heading != null) {
                        result.add(heading)
                    }
                    i++
                }

                // Horizontal rule: --- *** ___
                isHorizontalRule(trimmed) -> {
                    result.add(MarkdownNode.HorizontalRule)
                    i++
                }

                // Fenced code block: ```
                trimmed.startsWith("```") || trimmed.startsWith("~~~") -> {
                    val (codeBlock, consumed) = parseFencedCodeBlock(lines, i)
                    result.add(codeBlock)
                    i += consumed
                }

                // Custom block: :::
                trimmed.startsWith(":::") -> {
                    val (customBlock, consumed) = parseCustomBlock(lines, i)
                    if (customBlock != null) {
                        result.add(customBlock)
                    }
                    i += consumed
                }

                // Blockquote: >
                trimmed.startsWith(">") -> {
                    val (blockquote, consumed) = parseBlockquote(lines, i)
                    result.add(blockquote)
                    i += consumed
                }

                // Unordered list: - * +
                isUnorderedListStart(trimmed) -> {
                    val (list, consumed) = parseUnorderedList(lines, i)
                    result.add(list)
                    i += consumed
                }

                // Ordered list: 1. 2. etc
                isOrderedListStart(trimmed) -> {
                    val (list, consumed) = parseOrderedList(lines, i)
                    result.add(list)
                    i += consumed
                }

                // Table: starts with | (put - at start of char class for literal)
                trimmed.startsWith("|") || (i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|?[-\\s:]+\\|[-\\s:|]+\\|?$"))) -> {
                    val (table, consumed) = parseTable(lines, i)
                    if (table != null) {
                        result.add(table)
                        i += consumed
                    } else {
                        val (para, consumed2) = parseParagraph(lines, i)
                        result.add(para)
                        i += consumed2
                    }
                }

                // Default: paragraph
                else -> {
                    val (para, consumed) = parseParagraph(lines, i)
                    result.add(para)
                    i += consumed
                }
            }
        }

        return result
    }

    private fun parseHeading(line: String): MarkdownNode.Heading? {
        val match = Regex("^(#{1,6})\\s+(.*)$").find(line) ?: return null
        val level = match.groupValues[1].length
        val content = match.groupValues[2].trim()
        return MarkdownNode.Heading(level, parseInline(content))
    }

    private fun isHorizontalRule(line: String): Boolean {
        val trimmed = line.trim()
        // Must have at least 3 of the same character (-, *, or _), optionally with spaces
        if (trimmed.length < 3) return false
        val stripped = trimmed.replace(" ", "")
        if (stripped.length < 3) return false
        val first = stripped[0]
        if (first != '-' && first != '*' && first != '_') return false
        return stripped.all { it == first }
    }

    private fun parseFencedCodeBlock(lines: List<String>, startIndex: Int): Pair<MarkdownNode.CodeBlock, Int> {
        val firstLine = lines[startIndex].trim()
        val fence = if (firstLine.startsWith("```")) "```" else "~~~"
        val language = firstLine.removePrefix(fence).trim().takeIf { it.isNotEmpty() }

        val codeLines = mutableListOf<String>()
        var i = startIndex + 1

        while (i < lines.size) {
            val line = lines[i]
            if (line.trim().startsWith(fence)) {
                i++
                break
            }
            codeLines.add(line)
            i++
        }

        return MarkdownNode.CodeBlock(language, codeLines.joinToString("\n")) to (i - startIndex)
    }

    private fun parseCustomBlock(lines: List<String>, startIndex: Int): Pair<MarkdownNode?, Int> {
        val firstLine = lines[startIndex].trim()
        val afterColons = firstLine.removePrefix(":::").trim()

        if (afterColons.isEmpty()) {
            return null to 1
        }

        // Parse block type and attributes: "card href="/path" class="foo""
        val (blockType, attributes) = parseBlockTypeAndAttributes(afterColons)

        val contentLines = mutableListOf<String>()
        var i = startIndex + 1

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line == ":::") {
                i++
                break
            }
            contentLines.add(lines[i])
            i++
        }

        // Check for custom handler first
        val handler = customBlocks[blockType] ?: CustomBlockHandler.Default
        return handler.parse(blockType, attributes, contentLines.joinToString("\n"), this) to (i - startIndex)
    }

    /**
     * Parses a string like "card href="/path" class="foo"" into type and attributes.
     */
    private fun parseBlockTypeAndAttributes(input: String): Pair<String, Map<String, String>> {
        val parts = input.split(Regex("\\s+"), limit = 2)
        val blockType = parts[0]

        if (parts.size == 1) {
            return blockType to emptyMap()
        }

        val attributeString = parts[1]
        val attributes = mutableMapOf<String, String>()

        // Parse attributes like: href="/path" class="foo" or href='/path'
        val attrPattern = Regex("""(\w+)=["']([^"']*)["']""")
        attrPattern.findAll(attributeString).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            attributes[key] = value
        }

        return blockType to attributes
    }

    private fun parseBlockquote(lines: List<String>, startIndex: Int): Pair<MarkdownNode.Blockquote, Int> {
        val quoteLines = mutableListOf<String>()
        var i = startIndex

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.startsWith(">")) {
                // Remove > prefix and one optional space
                val content = trimmed.removePrefix(">").removePrefix(" ")
                quoteLines.add(content)
                i++
            } else if (trimmed.isEmpty() && i + 1 < lines.size && lines[i + 1].trim().startsWith(">")) {
                // Continue through blank lines if next line is still a quote
                quoteLines.add("")
                i++
            } else {
                break
            }
        }

        val children = parseBlocksInternal(quoteLines)
        return MarkdownNode.Blockquote(children) to (i - startIndex)
    }

    private fun isUnorderedListStart(line: String): Boolean {
        return line.matches(Regex("^[-*+]\\s+.*")) || line.matches(Regex("^[-*+]\\s*\\[[ xX]\\]\\s+.*"))
    }

    private fun isOrderedListStart(line: String): Boolean {
        return line.matches(Regex("^\\d+\\.\\s+.*"))
    }

    private fun isBlockLevelStart(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("#") ||
            trimmed.startsWith("[>") ||
            trimmed.startsWith(">") ||
            trimmed.startsWith("```") ||
            trimmed.startsWith("~~~") ||
            trimmed.startsWith(":::") ||
            isHorizontalRule(trimmed) ||
            isUnorderedListStart(trimmed) ||
            isOrderedListStart(trimmed)
    }

    private fun parseUnorderedList(lines: List<String>, startIndex: Int): Pair<MarkdownNode.UnorderedList, Int> {
        val items = mutableListOf<MarkdownNode>()
        var i = startIndex

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (!isUnorderedListStart(trimmed) && trimmed.isNotEmpty() && !line.startsWith("  ") && !line.startsWith("\t")) {
                break
            }

            if (isUnorderedListStart(trimmed)) {
                // Check for task list item
                val taskMatch = Regex("^[-*+]\\s*\\[([ xX])\\]\\s+(.*)$").find(trimmed)
                if (taskMatch != null) {
                    val checked = taskMatch.groupValues[1].lowercase() == "x"
                    val content = taskMatch.groupValues[2]
                    val (itemContent, consumed) = parseListItemContent(lines, i, content)
                    items.add(MarkdownNode.TaskListItem(checked, itemContent))
                    i += consumed
                } else {
                    // Regular list item
                    val content = trimmed.removePrefix("-").removePrefix("*").removePrefix("+").trim()
                    val (itemContent, consumed) = parseListItemContent(lines, i, content)
                    items.add(MarkdownNode.ListItem(itemContent))
                    i += consumed
                }
            } else if (trimmed.isEmpty()) {
                i++
            } else {
                break
            }
        }

        return MarkdownNode.UnorderedList(items.map {
            when (it) {
                is MarkdownNode.ListItemNode -> it
                else -> MarkdownNode.ListItem(listOf(it))
            }
        }.filterIsInstance<MarkdownNode.ListItemNode>()) to (i - startIndex)
    }

    private fun parseOrderedList(lines: List<String>, startIndex: Int): Pair<MarkdownNode.OrderedList, Int> {
        val items = mutableListOf<MarkdownNode.ListItem>()
        var i = startIndex
        var startNumber = 1

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (!isOrderedListStart(trimmed) && trimmed.isNotEmpty() && !line.startsWith("  ") && !line.startsWith("\t")) {
                break
            }

            if (isOrderedListStart(trimmed)) {
                val match = Regex("^(\\d+)\\.\\s+(.*)$").find(trimmed)
                if (match != null) {
                    if (items.isEmpty()) {
                        startNumber = match.groupValues[1].toIntOrNull() ?: 1
                    }
                    val content = match.groupValues[2]
                    val (itemContent, consumed) = parseListItemContent(lines, i, content)
                    items.add(MarkdownNode.ListItem(itemContent))
                    i += consumed
                } else {
                    i++
                }
            } else if (trimmed.isEmpty()) {
                i++
            } else {
                break
            }
        }

        return MarkdownNode.OrderedList(items, startNumber) to (i - startIndex)
    }

    private fun parseListItemContent(lines: List<String>, startIndex: Int, firstLineContent: String): Pair<List<MarkdownNode>, Int> {
        val contentLines = mutableListOf(firstLineContent)
        var i = startIndex + 1

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                trimmed.isEmpty() -> {
                    // Look ahead past consecutive blank lines to decide if this item continues
                    val nextNonEmpty = (i + 1 until lines.size).firstOrNull { lines[it].isNotBlank() }
                    if (nextNonEmpty == null) break
                    val nextLine = lines[nextNonEmpty]
                    val nextTrimmed = nextLine.trim()
                    if (!nextLine.startsWith("  ") && !nextLine.startsWith("\t") &&
                        !isUnorderedListStart(nextTrimmed) && !isOrderedListStart(nextTrimmed)) {
                        break
                    }
                    contentLines.add("")
                    i++
                }
                line.startsWith("  ") -> {
                    // Indented continuation — strip 2 spaces, preserving any deeper indentation for nested blocks
                    contentLines.add(line.removePrefix("  "))
                    i++
                }
                line.startsWith("\t") -> {
                    // Tab-indented continuation — strip one tab
                    contentLines.add(line.removePrefix("\t"))
                    i++
                }
                isUnorderedListStart(trimmed) || isOrderedListStart(trimmed) -> {
                    // Non-indented list item: sibling of this item, not a child
                    break
                }
                else -> break
            }
        }

        val joinedContent = contentLines.joinToString("\n").trim()

        // Fast path: single-line content that won't produce nested block elements
        if (!joinedContent.contains("\n") && !isBlockLevelStart(joinedContent)) {
            return listOf(MarkdownNode.Paragraph(parseInline(joinedContent))) to (i - startIndex)
        }

        return parseBlocksInternal(contentLines) to (i - startIndex)
    }

    private fun parseTable(lines: List<String>, startIndex: Int): Pair<MarkdownNode.Table?, Int> {
        if (startIndex + 1 >= lines.size) return null to 1

        val headerLine = lines[startIndex].trim()
        val separatorLine = lines[startIndex + 1].trim()

        // Validate separator line (must contain | and - and possibly :)
        // Note: Put - at start of character class to treat it as literal, not range
        if (!separatorLine.matches(Regex("^\\|?[-\\s:]+\\|[-\\s:|]+\\|?$"))) {
            return null to 1
        }

        // Parse alignments from separator
        val alignments = parseTableAlignments(separatorLine)

        // Parse header cells
        val headers = parseTableRow(headerLine)
        if (headers.isEmpty()) return null to 1

        // Parse body rows
        val rows = mutableListOf<List<List<MarkdownNode.InlineNode>>>()
        var i = startIndex + 2

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || !line.contains("|")) {
                break
            }
            val cells = parseTableRow(line)
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
            i++
        }

        return MarkdownNode.Table(headers, rows, alignments) to (i - startIndex)
    }

    private fun parseTableAlignments(separatorLine: String): List<Align?> {
        val cells = separatorLine.split("|").filter { it.isNotBlank() }
        return cells.map { cell ->
            val trimmed = cell.trim()
            val leftColon = trimmed.startsWith(":")
            val rightColon = trimmed.endsWith(":")
            when {
                leftColon && rightColon -> Align.Center
                rightColon -> Align.End
                leftColon -> Align.Start
                else -> null
            }
        }
    }

    private fun parseTableRow(line: String): List<List<MarkdownNode.InlineNode>> {
        val trimmed = line.trim().removeSurrounding("|")
        if (trimmed.isBlank()) return emptyList()
        return trimmed.split("|").map { cell -> parseInline(cell.trim()) }
    }

    private fun parseParagraph(lines: List<String>, startIndex: Int): Pair<MarkdownNode.Paragraph, Int> {
        val paragraphLines = mutableListOf<String>()
        var i = startIndex

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // End paragraph on empty line or block-level element
            if (trimmed.isEmpty() ||
                trimmed.startsWith("#") ||
                trimmed.startsWith(">") ||
                trimmed.startsWith("```") ||
                trimmed.startsWith("~~~") ||
                trimmed.startsWith(":::") ||
                isHorizontalRule(trimmed) ||
                isUnorderedListStart(trimmed) ||
                isOrderedListStart(trimmed) ||
                (trimmed.startsWith("|") && i + 1 < lines.size && lines[i + 1].contains("|") && lines[i + 1].contains("-"))
            ) {
                break
            }

            paragraphLines.add(trimmed)
            i++
        }

        val text = paragraphLines.joinToString(" ")
        return MarkdownNode.Paragraph(parseInline(text)) to (i - startIndex)
    }

    private fun parseBlockLink(lines: List<String>, startIndex: Int): Pair<MarkdownNode.BlockLink?, Int> {
        val firstLine = lines[startIndex].trim()

        // Single-line block link: [> content](url)
        val singleLineMatch = Regex("^\\[>\\s*(.+)\\]\\(([^)]+)\\)$").find(firstLine)
        if (singleLineMatch != null) {
            val content = singleLineMatch.groupValues[1]
            val url = singleLineMatch.groupValues[2]
            val children = parseBlocksInternal(listOf(content))
            return MarkdownNode.BlockLink(url, children) to 1
        }

        // Multi-line block link: [> ... ](url)
        if (firstLine == "[>" || firstLine.startsWith("[>") && !firstLine.contains("](")) {
            val contentLines = mutableListOf<String>()
            val firstContent = firstLine.removePrefix("[>").trim()
            if (firstContent.isNotEmpty()) {
                contentLines.add(firstContent)
            }

            var i = startIndex + 1
            while (i < lines.size) {
                val line = lines[i]
                val trimmed = line.trim()

                // Look for closing ](url)
                val closeMatch = Regex("^\\]\\(([^)]+)\\)$").find(trimmed)
                if (closeMatch != null) {
                    val url = closeMatch.groupValues[1]
                    val children = parseBlocksInternal(contentLines)
                    return MarkdownNode.BlockLink(url, children) to (i - startIndex + 1)
                }

                contentLines.add(line)
                i++
            }
        }

        return null to 1
    }

    // ================================
    // Inline parsing
    // ================================

    override fun parseInline(text: String): List<MarkdownNode.InlineNode> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<MarkdownNode.InlineNode>()
        var i = 0

        while (i < text.length) {
            when {
                // Escaped character
                text[i] == '\\' && i + 1 < text.length -> {
                    result.add(MarkdownNode.Text(text[i + 1].toString()))
                    i += 2
                }

                // Bold/Italic with **
                text.startsWith("***", i) -> {
                    val (node, consumed) = parseBoldItalic(text, i, "***")
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("*"))
                        i++
                    }
                }

                // Bold with **
                text.startsWith("**", i) -> {
                    val (node, consumed) = parseEmphasis(text, i, "**") { MarkdownNode.Bold(it) }
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("*"))
                        i++
                    }
                }

                // Italic with *
                text[i] == '*' -> {
                    val (node, consumed) = parseEmphasis(text, i, "*") { MarkdownNode.Italic(it) }
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("*"))
                        i++
                    }
                }

                // Bold with __
                text.startsWith("__", i) -> {
                    val (node, consumed) = parseEmphasis(text, i, "__") { MarkdownNode.Bold(it) }
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("_"))
                        i++
                    }
                }

                // Italic with _
                text[i] == '_' -> {
                    val (node, consumed) = parseEmphasis(text, i, "_") { MarkdownNode.Italic(it) }
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("_"))
                        i++
                    }
                }

                // Strikethrough ~~
                text.startsWith("~~", i) -> {
                    val (node, consumed) = parseEmphasis(text, i, "~~") { MarkdownNode.Strikethrough(it) }
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("~"))
                        i++
                    }
                }

                // Inline code `
                text[i] == '`' -> {
                    val (node, consumed) = parseInlineCode(text, i)
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("`"))
                        i++
                    }
                }

                // Image ![]()
                text.startsWith("![", i) -> {
                    val (node, consumed) = parseImage(text, i)
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("!"))
                        i++
                    }
                }

                // Link []()
                text[i] == '[' -> {
                    val (node, consumed) = parseLink(text, i)
                    if (node != null) {
                        result.add(node)
                        i += consumed
                    } else {
                        result.add(MarkdownNode.Text("["))
                        i++
                    }
                }

                // Hard line break (two spaces followed by newline, or backslash newline)
                text.startsWith("  \n", i) -> {
                    result.add(MarkdownNode.LineBreak)
                    i += 3
                }

                text.startsWith("\\\n", i) -> {
                    result.add(MarkdownNode.LineBreak)
                    i += 2
                }

                // Regular text (or unmatched special char)
                else -> {
                    val textEnd = findNextSpecialChar(text, i)
                    if (textEnd == i) {
                        // findNextSpecialChar found a special char at current position
                        // but none of the cases above matched, so treat it as text
                        result.add(MarkdownNode.Text(text[i].toString()))
                        i++
                    } else {
                        result.add(MarkdownNode.Text(text.substring(i, textEnd)))
                        i = textEnd
                    }
                }
            }
        }

        return result
    }

    private fun findNextSpecialChar(text: String, start: Int): Int {
        var i = start
        while (i < text.length) {
            when (text[i]) {
                '*', '_', '~', '`', '[', '!', '\\' -> return i
                ' ' -> {
                    if (i + 2 < text.length && text[i + 1] == ' ' && text[i + 2] == '\n') return i
                    i++
                }
                else -> i++
            }
        }
        return text.length
    }

    private fun parseBoldItalic(text: String, start: Int, marker: String): Pair<MarkdownNode.InlineNode?, Int> {
        val endIndex = text.indexOf(marker, start + marker.length)
        if (endIndex == -1) return null to 0

        val content = text.substring(start + marker.length, endIndex)
        val innerNodes = parseInline(content)
        // Bold containing italic
        return MarkdownNode.Bold(listOf(MarkdownNode.Italic(innerNodes))) to (endIndex - start + marker.length)
    }

    private fun parseEmphasis(
        text: String,
        start: Int,
        marker: String,
        factory: (List<MarkdownNode.InlineNode>) -> MarkdownNode.InlineNode
    ): Pair<MarkdownNode.InlineNode?, Int> {
        val endIndex = text.indexOf(marker, start + marker.length)
        if (endIndex == -1) return null to 0

        val content = text.substring(start + marker.length, endIndex)
        val innerNodes = parseInline(content)
        return factory(innerNodes) to (endIndex - start + marker.length)
    }

    private fun parseInlineCode(text: String, start: Int): Pair<MarkdownNode.InlineCode?, Int> {
        // Handle double backticks ``
        val isDouble = text.startsWith("``", start)
        val marker = if (isDouble) "``" else "`"

        val endIndex = text.indexOf(marker, start + marker.length)
        if (endIndex == -1) return null to 0

        val content = text.substring(start + marker.length, endIndex)
        return MarkdownNode.InlineCode(content) to (endIndex - start + marker.length)
    }

    private fun parseImage(text: String, start: Int): Pair<MarkdownNode.Image?, Int> {
        // ![alt](url) or ![alt|size](url)
        val altEndIndex = text.indexOf(']', start + 2)
        if (altEndIndex == -1) return null to 0

        val altContent = text.substring(start + 2, altEndIndex)

        if (altEndIndex + 1 >= text.length || text[altEndIndex + 1] != '(') {
            return null to 0
        }

        val urlEndIndex = text.indexOf(')', altEndIndex + 2)
        if (urlEndIndex == -1) return null to 0

        val url = text.substring(altEndIndex + 2, urlEndIndex)

        // Check for size in alt text: alt|32
        val sizeMatch = Regex("^(.*)\\|(\\d+)$").find(altContent)
        val (alt, size) = if (sizeMatch != null) {
            sizeMatch.groupValues[1] to sizeMatch.groupValues[2].toIntOrNull()?.px
        } else {
            altContent to null
        }

        return MarkdownNode.Image(url, alt, size) to (urlEndIndex - start + 1)
    }

    private fun parseLink(text: String, start: Int): Pair<MarkdownNode.Link?, Int> {
        // [text](url)
        val textEndIndex = text.indexOf(']', start + 1)
        if (textEndIndex == -1) return null to 0

        val linkText = text.substring(start + 1, textEndIndex)

        if (textEndIndex + 1 >= text.length || text[textEndIndex + 1] != '(') {
            return null to 0
        }

        val urlEndIndex = text.indexOf(')', textEndIndex + 2)
        if (urlEndIndex == -1) return null to 0

        val url = text.substring(textEndIndex + 2, urlEndIndex)
        val innerNodes = parseInline(linkText)

        return MarkdownNode.Link(url, innerNodes) to (urlEndIndex - start + 1)
    }
}
