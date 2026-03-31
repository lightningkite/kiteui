// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.ReactiveContext

/**
 * Renders markdown content as KiteUI views.
 *
 * @param content The markdown text to render
 * @param config Configuration for parsing and rendering
 */
@ViewDsl
fun ViewWriter.markdown(content: String, config: MarkdownConfig = MarkdownConfig.Default) {
    val parser = MarkdownParser(config.customBlocks)
    val document = parser.parse(content)
    renderDocument(document, config)
}

/**
 * Renders reactive markdown content, re-rendering when the content changes.
 *
 * @param content Lambda that returns markdown text (reactive)
 * @param config Configuration for parsing and rendering
 */
@ViewDsl
fun ViewWriter.markdownDynamic(content: ReactiveContext.() -> String, config: MarkdownConfig = MarkdownConfig.Default) {
    swapView {
        swapping(current = content) { md ->
            col { markdown(md, config) }
        }
    }
}

/**
 * Internal: Renders a parsed document.
 */
private fun ViewWriter.renderDocument(document: MarkdownNode.Document, config: MarkdownConfig) {
    col {
        document.children.forEach { child ->
            renderBlock(child, config)
        }
    }
}

// by Claude - MarkdownRenderContext implementation for custom block handlers
private class RenderContextImpl(
    override val config: MarkdownConfig
) : MarkdownRenderContext {
    override fun ViewWriter.renderNode(node: MarkdownNode) {
        renderBlock(node, config)
    }
}

/**
 * Internal: Renders a single block-level node.
 */
private fun ViewWriter.renderBlock(node: MarkdownNode, config: MarkdownConfig) {
    when (node) {
        is MarkdownNode.Document -> renderDocument(node, config)

        is MarkdownNode.Heading -> {
            val semantic = when (node.level) {
                1 -> HeaderSemantic + H1Semantic
                2 -> HeaderSemantic + H2Semantic
                3 -> HeaderSemantic + H3Semantic
                4 -> HeaderSemantic + H4Semantic
                5 -> HeaderSemantic + H5Semantic
                else -> HeaderSemantic + H6Semantic
            }
            renderInlineContentWithSemantic(node.content, config, semantic)
        }

        is MarkdownNode.Paragraph -> {
            renderInlineContent(node.content, config)
        }

        is MarkdownNode.CodeBlock -> {
            themed(CodeBlockSemantic).col {
                text {
                    content = node.code
                    wraps = true
                }
            }
        }

        is MarkdownNode.Blockquote -> {
            themed(BlockquoteSemantic).col {
                node.children.forEach { child ->
                    renderBlock(child, config)
                }
            }
        }

        is MarkdownNode.UnorderedList -> {
            col {
                node.items.forEach { item ->
                    renderListItem(item, bullet = "\u2022", config = config)
                }
            }
        }

        is MarkdownNode.OrderedList -> {
            col {
                node.items.forEachIndexed { index, item ->
                    renderListItem(item, bullet = "${node.startNumber + index}.", config = config)
                }
            }
        }

        is MarkdownNode.ListItem -> {
            // List items are rendered via renderListItem
            col {
                node.children.forEach { child ->
                    renderBlock(child, config)
                }
            }
        }

        is MarkdownNode.TaskListItem -> {
            row {
                checkbox {
                    checked.value = node.checked
                    enabled = false // Display only
                }
                col {
                    node.children.forEach { child ->
                        renderBlock(child, config)
                    }
                }
            }
        }

        is MarkdownNode.HorizontalRule -> {
            themed(HorizontalRuleSemantic).sizeConstraints(height = 2.px).frame { }
        }

        is MarkdownNode.Table -> renderTable(node, config)

        is MarkdownNode.CustomBlock -> {
            val handler = config.customBlocks[node.type] ?: CustomBlockHandler.Default
            val renderContext = RenderContextImpl(config)
            with(handler) {
                render(node.children, node.attributes, renderContext)
            }
        }

        is MarkdownNode.BlockLink -> {
            val page = config.resolveInternalLink(node.url)
            if (page != null) {
                link {
                    to = { page }
                    col {
                        node.children.forEach { child ->
                            renderBlock(child, config)
                        }
                    }
                }
            } else {
                externalLink {
                    to = node.url
                    col {
                        node.children.forEach { child ->
                            renderBlock(child, config)
                        }
                    }
                }
            }
        }

        // Inline nodes shouldn't appear at block level, but handle gracefully
        is MarkdownNode.InlineNode -> {
            renderInlineContent(listOf(node), config)
        }
    }
}

/**
 * Internal: Renders a list item with a bullet/number marker.
 */
private fun ViewWriter.renderListItem(item: MarkdownNode.ListItemNode, bullet: String, config: MarkdownConfig) {
    when (item) {
        is MarkdownNode.TaskListItem -> {
            row {
                checkbox {
                    checked.value = item.checked
                    enabled = false // Display only
                }
                expanding.col {
                    item.children.forEach { child ->
                        renderBlock(child, config)
                    }
                }
            }
        }
        is MarkdownNode.ListItem -> {
            row {
                themed(ListMarkerSemantic).text {
                    content = bullet
                }
                expanding.col {
                    item.children.forEach { child ->
                        renderBlock(child, config)
                    }
                }
            }
        }
    }
}

/**
 * Internal: Renders a table.
 */
private fun ViewWriter.renderTable(table: MarkdownNode.Table, config: MarkdownConfig) {
    col {
        // Header row
        row {
            table.headers.forEachIndexed { colIndex, headerCells ->
                val alignment = table.alignments.getOrNull(colIndex)
                (TableCellSemantic + TableHeaderSemantic).onNext.expanding.col {
                    renderTableCell(headerCells, alignment, config)
                }
            }
        }

        // Body rows
        table.rows.forEach { rowCells ->
            row {
                rowCells.forEachIndexed { colIndex, cellContent ->
                    val alignment = table.alignments.getOrNull(colIndex)
                    TableCellSemantic.onNext.expanding.col {
                        renderTableCell(cellContent, alignment, config)
                    }
                }
            }
        }
    }
}

/**
 * Internal: Renders a table cell, handling images properly.
 */
private fun ViewWriter.renderTableCell(
    content: List<MarkdownNode.InlineNode>,
    alignment: Align?,
    config: MarkdownConfig
) {
    if (!containsImages(content)) {
        text {
            if (alignment != null) this.align = alignment
            setBasicHtmlContent(inlineNodesToHtml(content, config))
        }
    } else {
        row {
            renderInlineSegments(content, config)
        }
    }
}

// ================================
// Inline content rendering
// ================================

/**
 * Checks if any inline nodes contain images (recursively).
 */
private fun containsImages(nodes: List<MarkdownNode.InlineNode>): Boolean {
    return nodes.any { node ->
        when (node) {
            is MarkdownNode.Image -> true
            is MarkdownNode.Bold -> containsImages(node.children)
            is MarkdownNode.Italic -> containsImages(node.children)
            is MarkdownNode.Strikethrough -> containsImages(node.children)
            is MarkdownNode.Link -> containsImages(node.children)
            else -> false
        }
    }
}

/**
 * Renders inline content. If images are present, uses a row with mixed text/image elements.
 * Otherwise uses a simple text element with HTML.
 */
private fun ViewWriter.renderInlineContent(nodes: List<MarkdownNode.InlineNode>, config: MarkdownConfig) {
    if (!containsImages(nodes)) {
        // No images - use simple HTML text
        text {
            setBasicHtmlContent(inlineNodesToHtml(nodes, config))
        }
    } else {
        // Has images - render as row with segments
        row {
            renderInlineSegments(nodes, config)
        }
    }
}

/**
 * Renders inline content with a semantic modifier applied.
 */
private fun ViewWriter.renderInlineContentWithSemantic(
    nodes: List<MarkdownNode.InlineNode>,
    config: MarkdownConfig,
    semantic: ThemeDerivation
) {
    if (!containsImages(nodes)) {
        themed(semantic).text {
            setBasicHtmlContent(inlineNodesToHtml(nodes, config))
        }
    } else {
        themed(semantic).row {
            renderInlineSegments(nodes, config)
        }
    }
}

/**
 * Renders inline segments, splitting at images.
 * Groups consecutive non-image nodes into text elements, renders images as image views.
 */
private fun ViewWriter.renderInlineSegments(nodes: List<MarkdownNode.InlineNode>, config: MarkdownConfig) {
    val segments = mutableListOf<Any>() // Either List<InlineNode> for text or Image node
    var currentTextNodes = mutableListOf<MarkdownNode.InlineNode>()

    fun flushText() {
        if (currentTextNodes.isNotEmpty()) {
            segments.add(currentTextNodes.toList())
            currentTextNodes = mutableListOf()
        }
    }

    // Flatten and split at images
    fun processNode(node: MarkdownNode.InlineNode) {
        when (node) {
            is MarkdownNode.Image -> {
                flushText()
                segments.add(node)
            }
            is MarkdownNode.Bold -> {
                if (containsImages(node.children)) {
                    // Image inside bold - need to split
                    flushText()
                    node.children.forEach { processNode(it) }
                } else {
                    currentTextNodes.add(node)
                }
            }
            is MarkdownNode.Italic -> {
                if (containsImages(node.children)) {
                    flushText()
                    node.children.forEach { processNode(it) }
                } else {
                    currentTextNodes.add(node)
                }
            }
            is MarkdownNode.Strikethrough -> {
                if (containsImages(node.children)) {
                    flushText()
                    node.children.forEach { processNode(it) }
                } else {
                    currentTextNodes.add(node)
                }
            }
            is MarkdownNode.Link -> {
                if (containsImages(node.children)) {
                    flushText()
                    node.children.forEach { processNode(it) }
                } else {
                    currentTextNodes.add(node)
                }
            }
            else -> currentTextNodes.add(node)
        }
    }

    nodes.forEach { processNode(it) }
    flushText()

    // Render each segment
    segments.forEach { segment ->
        when (segment) {
            is MarkdownNode.Image -> {
                val sizeConstraint = segment.size
                if (sizeConstraint != null) {
                    sizeConstraints(width = sizeConstraint, height = sizeConstraint).image {
                        source = ImageRemote(segment.url)
                        description = segment.alt
                    }
                } else {
                    image {
                        source = ImageRemote(segment.url)
                        description = segment.alt
                    }
                }
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val textNodes = segment as List<MarkdownNode.InlineNode>
                val html = inlineNodesToHtml(textNodes, config)
                if (html.isNotBlank()) {
                    text {
                        setBasicHtmlContent(html)
                    }
                }
            }
        }
    }
}

/**
 * Converts a list of inline nodes to HTML for setBasicHtmlContent().
 * Only used for non-image content.
 */
private fun inlineNodesToHtml(nodes: List<MarkdownNode.InlineNode>, config: MarkdownConfig): String {
    return nodes.joinToString("") { inlineNodeToHtml(it, config) }
}

/**
 * Converts a single inline node to HTML.
 */
private fun inlineNodeToHtml(node: MarkdownNode.InlineNode, config: MarkdownConfig): String {
    return when (node) {
        is MarkdownNode.Text -> escapeHtml(node.content)

        is MarkdownNode.Bold -> "<b>${inlineNodesToHtml(node.children, config)}</b>"

        is MarkdownNode.Italic -> "<i>${inlineNodesToHtml(node.children, config)}</i>"

        is MarkdownNode.Strikethrough -> "<s>${inlineNodesToHtml(node.children, config)}</s>"

        is MarkdownNode.InlineCode -> "<tt>${escapeHtml(node.content)}</tt>"

        is MarkdownNode.Link -> {
            val escapedUrl = escapeHtml(node.url)
            val innerHtml = inlineNodesToHtml(node.children, config)
            "<a href=\"$escapedUrl\">$innerHtml</a>"
        }

        is MarkdownNode.Image -> {
            // Images should be handled separately, but include placeholder text if this is called
            "[${escapeHtml(node.alt)}]"
        }

        is MarkdownNode.LineBreak -> "<br />"
    }
}

/**
 * Escapes HTML special characters.
 */
private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
