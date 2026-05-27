package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A rich text editor that supports Markdown serialization and deserialization.
 * Supports basic formatting like bold, italic, headers, and lists.
 */
expect class MarkdownRichTextEditor(context: ElementContext) : NativeContainerElement, ElementWithAction {
    var hint: String
    val content: MutableReactiveValue<String>
    var suggestionHandler: SuggestionHandler?
}

/**
 * Handler for suggestions (e.g., mentions, hashtags) in the editor.
 */
interface SuggestionHandler {
    // Currently a stub for future extension
}

/**
 * Enumeration of supported rich text formatting tags.
 */
enum class RichTextTags {
    HEADER1,
    HEADER2,
    HEADER3,
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    SUPERSCRIPT,
    SUBSCRIPT,
    LINK,
    UNORDERED_LIST,
    ORDERED_LIST,
    QOUTE,
    CODE,
    CODE_BLOCK
}
