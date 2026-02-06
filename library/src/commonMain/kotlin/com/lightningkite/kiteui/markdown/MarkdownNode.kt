// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension

/**
 * AST representation of parsed markdown content.
 * Supports both block-level and inline elements.
 */
sealed interface MarkdownNode {
    // Block-level nodes

    /**
     * Root document container holding all parsed markdown content.
     */
    data class Document(val children: List<MarkdownNode>) : MarkdownNode

    /**
     * Heading element (H1-H6).
     * @param level Heading level from 1-6
     * @param content Inline content within the heading
     */
    data class Heading(val level: Int, val content: List<InlineNode>) : MarkdownNode

    /**
     * Paragraph of text containing inline elements.
     */
    data class Paragraph(val content: List<InlineNode>) : MarkdownNode

    /**
     * Fenced or indented code block.
     * @param language Optional language hint for syntax highlighting
     * @param code The raw code content
     */
    data class CodeBlock(val language: String?, val code: String) : MarkdownNode

    /**
     * Block quote (lines starting with >).
     * Can contain nested block elements.
     */
    data class Blockquote(val children: List<MarkdownNode>) : MarkdownNode

    /**
     * Common interface for list items (regular and task list items).
     */
    sealed interface ListItemNode : MarkdownNode {
        val children: List<MarkdownNode>
    }

    /**
     * Unordered list (bullet points).
     */
    data class UnorderedList(val items: List<ListItemNode>) : MarkdownNode

    /**
     * Ordered list (numbered items).
     * @param startNumber The starting number for the list (usually 1)
     */
    data class OrderedList(val items: List<ListItem>, val startNumber: Int = 1) : MarkdownNode

    /**
     * A single list item which can contain nested block elements.
     */
    data class ListItem(override val children: List<MarkdownNode>) : ListItemNode

    /**
     * Task list item with a checkbox.
     * @param checked Whether the checkbox is checked
     */
    data class TaskListItem(val checked: Boolean, override val children: List<MarkdownNode>) : ListItemNode

    /**
     * Horizontal rule / thematic break (---, ***, ___).
     */
    data object HorizontalRule : MarkdownNode

    /**
     * Table with headers, rows, and column alignments.
     */
    data class Table(
        val headers: List<List<InlineNode>>,
        val rows: List<List<List<InlineNode>>>,
        val alignments: List<Align?>
    ) : MarkdownNode

    /**
     * Custom fenced block for extensions (e.g., ::: card).
     * @param type The custom block type identifier
     * @param attributes Key-value attributes (e.g., href="/path")
     * @param children Block content within the custom block
     */
    data class CustomBlock(
        val type: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<MarkdownNode>
    ) : MarkdownNode

    /**
     * Block-level link wrapping other block elements.
     * Syntax: [> block content](url) or multi-line variant.
     */
    data class BlockLink(val url: String, val children: List<MarkdownNode>) : MarkdownNode

    // Inline nodes

    /**
     * Marker interface for inline (non-block) content.
     */
    sealed interface InlineNode : MarkdownNode

    /**
     * Plain text content.
     */
    data class Text(val content: String) : InlineNode

    /**
     * Bold/strong text (**text** or __text__).
     */
    data class Bold(val children: List<InlineNode>) : InlineNode

    /**
     * Italic/emphasized text (*text* or _text_).
     */
    data class Italic(val children: List<InlineNode>) : InlineNode

    /**
     * Strikethrough text (~~text~~).
     */
    data class Strikethrough(val children: List<InlineNode>) : InlineNode

    /**
     * Inline code (`code`).
     */
    data class InlineCode(val content: String) : InlineNode

    /**
     * Inline hyperlink [text](url).
     */
    data class Link(val url: String, val children: List<InlineNode>) : InlineNode

    /**
     * Image element ![alt](url) with optional size hint.
     * Extended syntax ![alt|size](url) for sized images.
     */
    data class Image(val url: String, val alt: String, val size: Dimension? = null) : InlineNode

    /**
     * Hard line break (two spaces at end of line or backslash).
     */
    data object LineBreak : InlineNode
}
