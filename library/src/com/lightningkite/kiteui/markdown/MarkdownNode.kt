// by Claude
package com.lightningkite.kiteui.markdown

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension

/**
 * AST representation of parsed markdown content.
 * Supports both block-level and inline elements.
 */
public sealed interface MarkdownNode {
    // Block-level nodes

    /**
     * Root document container holding all parsed markdown content.
     */
    public data class Document(val children: List<MarkdownNode>) : MarkdownNode

    /**
     * Heading element (H1-H6).
     * @param level Heading level from 1-6
     * @param content Inline content within the heading
     */
    public data class Heading(val level: Int, val content: List<InlineNode>) : MarkdownNode

    /**
     * Paragraph of text containing inline elements.
     */
    public data class Paragraph(val content: List<InlineNode>) : MarkdownNode

    /**
     * Fenced or indented code block.
     * @param language Optional language hint for syntax highlighting
     * @param code The raw code content
     */
    public data class CodeBlock(val language: String?, val code: String) : MarkdownNode

    /**
     * Block quote (lines starting with >).
     * Can contain nested block elements.
     */
    public data class Blockquote(val children: List<MarkdownNode>) : MarkdownNode

    /**
     * Common interface for list items (regular and task list items).
     */
    public sealed interface ListItemNode : MarkdownNode {
        public val children: List<MarkdownNode>
    }

    /**
     * Unordered list (bullet points).
     */
    public data class UnorderedList(val items: List<ListItemNode>) : MarkdownNode

    /**
     * Ordered list (numbered items).
     * @param startNumber The starting number for the list (usually 1)
     */
    public data class OrderedList(val items: List<ListItem>, val startNumber: Int = 1) : MarkdownNode

    /**
     * A single list item which can contain nested block elements.
     */
    public data class ListItem(override val children: List<MarkdownNode>) : ListItemNode

    /**
     * Task list item with a checkbox.
     * @param checked Whether the checkbox is checked
     */
    public data class TaskListItem(val checked: Boolean, override val children: List<MarkdownNode>) : ListItemNode

    /**
     * Horizontal rule / thematic break (---, ***, ___).
     */
    public data object HorizontalRule : MarkdownNode

    /**
     * Table with headers, rows, and column alignments.
     */
    public data class Table(
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
    public data class CustomBlock(
        val type: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<MarkdownNode>
    ) : MarkdownNode

    /**
     * Block-level link wrapping other block elements.
     * Syntax: [> block content](url) or multi-line variant.
     */
    public data class BlockLink(val url: String, val children: List<MarkdownNode>) : MarkdownNode

    // Inline nodes

    /**
     * Marker interface for inline (non-block) content.
     */
    public sealed interface InlineNode : MarkdownNode

    /**
     * Plain text content.
     */
    public data class Text(val content: String) : InlineNode

    /**
     * Bold/strong text (**text** or __text__).
     */
    public data class Bold(val children: List<InlineNode>) : InlineNode

    /**
     * Italic/emphasized text (*text* or _text_).
     */
    public data class Italic(val children: List<InlineNode>) : InlineNode

    /**
     * Strikethrough text (~~text~~).
     */
    public data class Strikethrough(val children: List<InlineNode>) : InlineNode

    /**
     * Inline code (`code`).
     */
    public data class InlineCode(val content: String) : InlineNode

    /**
     * Inline hyperlink [text](url).
     */
    public data class Link(val url: String, val children: List<InlineNode>) : InlineNode

    /**
     * Image element ![alt](url) with optional size hint.
     * Extended syntax ![alt|size](url) for sized images.
     */
    public data class Image(val url: String, val alt: String, val size: Dimension? = null) : InlineNode

    /**
     * Hard line break (two spaces at end of line or backslash).
     */
    public data object LineBreak : InlineNode
}
