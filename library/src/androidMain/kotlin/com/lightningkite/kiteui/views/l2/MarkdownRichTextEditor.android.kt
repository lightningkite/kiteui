package com.lightningkite.kiteui.views.l2

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.bold
import com.lightningkite.kiteui.models.code
import com.lightningkite.kiteui.models.codeBlock
import com.lightningkite.kiteui.models.header1
import com.lightningkite.kiteui.models.header2
import com.lightningkite.kiteui.models.header3
import com.lightningkite.kiteui.models.italic
import com.lightningkite.kiteui.models.orderList
import com.lightningkite.kiteui.models.qoute
import com.lightningkite.kiteui.models.strikeThrough
import com.lightningkite.kiteui.models.unorderedList
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.markdown.MarkdownNode
import com.lightningkite.kiteui.markdown.MarkdownParser
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch

@OptIn(com.lightningkite.kiteui.UnsafeModifier::class)
actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {

    override val native = LinearLayout(context.activity)

    val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())

    actual var suggestionHandler: SuggestionHandler? = null

    // Native Android EditText to handle the typing and spans
    val nativeEditText = EditText(context.activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        background = null // Remove default underline
        gravity = Gravity.TOP or Gravity.START
        minLines = 3
    }

    init {
        // Setup Native Container as Vertical LinearLayout
        val viewGroup = native as? LinearLayout
        viewGroup?.orientation = LinearLayout.VERTICAL

        // Build the Toolbar exactly as you did in JS
        scrollingHorizontally.row {
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.HEADER1)
                    ) SelectedSemantic else null
                }
                icon(Icon.header1, "Header 1")
                onClick { this@MarkdownRichTextEditor.toggleSpan(RelativeSizeSpan(2.0f), RichTextTags.HEADER1) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.HEADER2)
                    ) SelectedSemantic else null
                }
                icon(Icon.header2, "Header 2")
                onClick { this@MarkdownRichTextEditor.toggleSpan(RelativeSizeSpan(1.5f), RichTextTags.HEADER2) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.HEADER3)
                    ) SelectedSemantic else null
                }
                icon(Icon.header3, "Header 3")
                onClick { this@MarkdownRichTextEditor.toggleSpan(RelativeSizeSpan(1.17f), RichTextTags.HEADER3) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.BOLD)
                    ) SelectedSemantic else null
                }
                icon(Icon.bold, "Bold")
                onClick { this@MarkdownRichTextEditor.toggleStyleSpan(Typeface.BOLD, RichTextTags.BOLD) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.ITALIC)
                    ) SelectedSemantic else null
                }
                icon(Icon.italic, "Italic")
                onClick { this@MarkdownRichTextEditor.toggleStyleSpan(Typeface.ITALIC, RichTextTags.ITALIC) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.STRIKETHROUGH)
                    ) SelectedSemantic else null
                }
                icon(Icon.strikeThrough, "Strikethrough")
                onClick { this@MarkdownRichTextEditor.toggleSpan(StrikethroughSpan(), RichTextTags.STRIKETHROUGH) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.QOUTE)
                    ) SelectedSemantic else null
                }
                icon(Icon.qoute, "Quote")
                onClick { /* Implement Quote Span */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.UNORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.unorderedList, "Unordered List")
                onClick { this@MarkdownRichTextEditor.insertList(ordered = false) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.ORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.orderList, "Ordered List")
                onClick { this@MarkdownRichTextEditor.insertList(ordered = true) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.CODE)
                    ) SelectedSemantic else null
                }
                icon(Icon.code, "Code")
                onClick { this@MarkdownRichTextEditor.toggleSpan(TypefaceSpan("monospace"), RichTextTags.CODE) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.CODE_BLOCK)
                    ) SelectedSemantic else null
                }
                icon(Icon.codeBlock, "Code Block")
                onClick { /* Implement Code Block Span */ }
            }
        }

        // Add the editor to the native view
        viewGroup?.addView(nativeEditText)

        // Listeners for Cursor and Content updates
        nativeEditText.setOnClickListener { updateSelectedTags() }

        nativeEditText.addTextChangedListener(object : TextWatcher {
            var isEnter = false
            var enterPos = -1
            var recursive = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (recursive) return
                // Robust enter detection: was a newline just added?
                isEnter = count == 1 && s?.get(start) == '\n'
                enterPos = start
                updateSelectedTags()
            }
            override fun afterTextChanged(s: Editable?) {
                if (recursive || s == null) return
                if (isEnter) {
                    isEnter = false
                    val pos = enterPos
                    val currentLineStart = if (pos > 0) {
                        val lastNewline = s.lastIndexOf('\n', pos - 1)
                        if (lastNewline == -1) 0 else lastNewline + 1
                    } else 0

                    val lineText = s.substring(currentLineStart, pos)

                    if (lineText.startsWith("* ")) {
                        // Unordered list continuation/exit
                        if (lineText == "* ") {
                            // Empty - exit
                            recursive = true
                            s.delete(currentLineStart, pos + 1)
                            recursive = false
                        } else {
                            // Continue
                            recursive = true
                            s.insert(pos + 1, "* ")
                            recursive = false
                        }
                    } else if (lineText.trim().matches(Regex("^\\d+\\.\\s+.*"))) {
                        // Ordered list continuation/exit
                        val match = Regex("^(\\d+)\\.\\s+").find(lineText)
                        if (match != null) {
                            val num = match.groupValues[1].toInt()
                            if (lineText.trim().length <= match.groupValues[0].length) {
                                // Empty - exit
                                recursive = true
                                s.delete(currentLineStart, pos + 1)
                                recursive = false
                            } else {
                                // Continue
                                recursive = true
                                s.insert(pos + 1, "${num + 1}. ")
                                recursive = false
                            }
                        }
                    } else {
                        // Not in a list - Reset modifiers for the NEXT line
                        // To be truly robust, we truncate spans at 'pos' and explicitly
                        // clear them from 'pos + 1' onwards.
                        val activeSpans = s.getSpans(pos, pos, Any::class.java)
                        activeSpans.forEach { span ->
                            if (span is StyleSpan || span is StrikethroughSpan || span is TypefaceSpan || span is RelativeSizeSpan) {
                                val startSpan = s.getSpanStart(span)
                                if (startSpan != -1 && startSpan < pos) {
                                    s.setSpan(span, startSpan, pos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                            }
                        }

                        // Also clear any spans that might have expanded to pos + 1
                        val nextSpans = s.getSpans(pos + 1, pos + 1, Any::class.java)
                        nextSpans.forEach { span ->
                            if (span is StyleSpan || span is StrikethroughSpan || span is TypefaceSpan || span is RelativeSizeSpan) {
                                if (s.getSpanStart(span) == pos + 1 && s.getSpanEnd(span) == pos + 1) {
                                    s.removeSpan(span)
                                }
                            }
                        }
                    }
                }
                (content as? ContentValue)?.update()
            }
        })
    }

    actual var hint: String
        get() = nativeEditText.hint?.toString() ?: ""
        set(value) {
            nativeEditText.hint = value
        }

    actual val content: MutableReactiveValue<String> = ContentValue()

    // BaseListenable Binder to sync string updates
    inner class ContentValue : MutableReactiveValue<String>, BaseListenable() {
        fun update() = invokeAllListeners()
        override var value: String
            get() = serializeToMarkdown(nativeEditText.text)
            set(value) {
                if (serializeToMarkdown(nativeEditText.text) != value) {
                    val isFocused = nativeEditText.hasFocus()
                    val cursor = nativeEditText.selectionStart

                    nativeEditText.setText(parseMarkdownToSpannable(value))

                    if (isFocused && cursor >= 0 && cursor <= nativeEditText.text.length) {
                        nativeEditText.setSelection(cursor)
                    }
                    updateSelectedTags()
                }
            }

    }

    // --- Format Toggling Logic ---

    private fun createSimilarSpan(existing: Any): Any? {
        return when (existing) {
            is RelativeSizeSpan -> RelativeSizeSpan(existing.sizeChange)
            is StrikethroughSpan -> StrikethroughSpan()
            is TypefaceSpan -> TypefaceSpan(existing.family)
            is StyleSpan -> StyleSpan(existing.style)
            is UnderlineSpan -> UnderlineSpan()
            else -> null
        }
    }

    private fun toggleStyleSpan(style: Int, tag: RichTextTags) {
        val start = nativeEditText.selectionStart
        val end = nativeEditText.selectionEnd
        if (start < 0) return

        val spannable = nativeEditText.text ?: return

        val spans = if (start == end) {
            spannable.getSpans(start, start, StyleSpan::class.java).filter { it.style == style }
        } else {
            spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == style }
        }

        if (spans.isNotEmpty()) {
            // Turning OFF or removing from selection
            spans.forEach { span ->
                val s = spannable.getSpanStart(span)
                val e = spannable.getSpanEnd(span)

                if (s < start) {
                    // Truncate left side
                    spannable.setSpan(span, s, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (e > end) {
                        // Re-add right side
                        spannable.setSpan(StyleSpan(style), end, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } else if (e > end) {
                    // Truncate right side
                    spannable.setSpan(span, end, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    // Fully within selection
                    spannable.removeSpan(span)
                }
            }
        } else {
            // Turning ON
            if (start == end) {
                // Use INCLUSIVE_INCLUSIVE so it expands as we type
                spannable.setSpan(StyleSpan(style), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                spannable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    private fun toggleSpan(span: Any, tag: RichTextTags) {
        val start = nativeEditText.selectionStart
        val end = nativeEditText.selectionEnd
        if (start < 0) return

        val spannable = nativeEditText.text ?: return

        val spans = if (start == end) {
            spannable.getSpans(start, start, span.javaClass).filter {
                when {
                    it is RelativeSizeSpan && span is RelativeSizeSpan -> it.sizeChange == span.sizeChange
                    it is TypefaceSpan && span is TypefaceSpan -> it.family == span.family
                    else -> true
                }
            }
        } else {
            spannable.getSpans(start, end, span.javaClass).filter {
                when {
                    it is RelativeSizeSpan && span is RelativeSizeSpan -> it.sizeChange == span.sizeChange
                    it is TypefaceSpan && span is TypefaceSpan -> it.family == span.family
                    else -> true
                }
            }
        }

        if (spans.isNotEmpty()) {
            // Turning OFF or removing from selection
            spans.forEach { existing ->
                val s = spannable.getSpanStart(existing)
                val e = spannable.getSpanEnd(existing)

                if (s < start) {
                    // Truncate left side
                    spannable.setSpan(existing, s, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (e > end) {
                        // Re-add right side
                        createSimilarSpan(existing)?.let {
                            spannable.setSpan(it, end, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                } else if (e > end) {
                    // Truncate right side
                    spannable.setSpan(existing, end, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    // Fully within selection
                    spannable.removeSpan(existing)
                }
            }
        } else {
            // Turning ON
            if (start == end) {
                spannable.setSpan(span, start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    private fun insertList(ordered: Boolean) {
        val start = nativeEditText.selectionStart
        val end = nativeEditText.selectionEnd
        if (start < 0) return

        val spannable = nativeEditText.text ?: return

        val currentLineStart = if (start > 0) {
            val lastNewline = spannable.lastIndexOf('\n', start - 1)
            if (lastNewline == -1) 0 else lastNewline + 1
        } else 0

        if (ordered) {
            spannable.insert(currentLineStart, "1. ")
        } else {
            spannable.insert(currentLineStart, "* ")
        }

        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    // --- Tag Detection ---

    private fun updateSelectedTags() {
        val start = nativeEditText.selectionStart
        if (start < 0) return

        val spannable = nativeEditText.text ?: return
        val tags = mutableSetOf<RichTextTags>()

        // Grab spans immediately around the cursor
        val spans = spannable.getSpans(start, start, Any::class.java)

        spans.forEach { span ->
            when (span) {
                is StyleSpan -> {
                    if (span.style == Typeface.BOLD) tags.add(RichTextTags.BOLD)
                    if (span.style == Typeface.ITALIC) tags.add(RichTextTags.ITALIC)
                }

                is StrikethroughSpan -> tags.add(RichTextTags.STRIKETHROUGH)
                is RelativeSizeSpan -> {
                    when (span.sizeChange) {
                        2.0f -> tags.add(RichTextTags.HEADER1)
                        1.5f -> tags.add(RichTextTags.HEADER2)
                        1.17f -> tags.add(RichTextTags.HEADER3)
                    }
                }

                is TypefaceSpan -> {
                    if (span.family == "monospace") tags.add(RichTextTags.CODE)
                }
            }
        }

        if (currentSelectedRichTextTags.value != tags) {
            currentSelectedRichTextTags.value = tags
        }
    }

    // --- Markdown Conversion ---

    private fun serializeToMarkdown(spannable: Editable?): String {
        if (spannable == null || spannable.isEmpty()) return ""
        val length = spannable.length
        val result = StringBuilder()

        var currentStyles = emptyList<String>()
        val styleOrder = listOf("~~", "**", "*", "`")

        for (i in 0 until length) {
            val char = spannable[i]

            // Headings check at start of line
            if (i == 0 || spannable[i - 1] == '\n') {
                val lineSpans = spannable.getSpans(i, i + 1, RelativeSizeSpan::class.java)
                val headingSpan = lineSpans.firstOrNull { it.sizeChange > 1.0f }
                if (headingSpan != null) {
                    val level = when {
                        headingSpan.sizeChange >= 2.0f -> 1
                        headingSpan.sizeChange >= 1.5f -> 2
                        headingSpan.sizeChange >= 1.17f -> 3
                        else -> 0
                    }
                    if (level > 0) {
                        for (l in 0 until level) result.append("#")
                        result.append(" ")
                    }
                }
            }

            val spans = spannable.getSpans(i, i + 1, Any::class.java)
            val stylesAtI = mutableSetOf<String>()
            spans.forEach { span ->
                if (spannable.getSpanStart(span) <= i && spannable.getSpanEnd(span) >= i + 1) {
                    when (span) {
                        is StyleSpan -> {
                            if (span.style == Typeface.BOLD) stylesAtI.add("**")
                            if (span.style == Typeface.ITALIC) stylesAtI.add("*")
                        }

                        is StrikethroughSpan -> stylesAtI.add("~~")
                        is TypefaceSpan -> if (span.family == "monospace") stylesAtI.add("`")
                    }
                }
            }

            // Close styles that are no longer active
            val activeButShouldBeClosed = currentStyles.filter { it !in stylesAtI }
            if (activeButShouldBeClosed.isNotEmpty()) {
                currentStyles.reversed().forEach { result.append(it) }
                currentStyles = emptyList()
            }

            // Open styles that should be active
            styleOrder.forEach { style ->
                if (style in stylesAtI && style !in currentStyles) {
                    result.append(style)
                    currentStyles = currentStyles + style
                }
            }

            result.append(char)
        }

        currentStyles.reversed().forEach { result.append(it) }

        return result.toString()
    }

    private fun parseMarkdownToSpannable(markdown: String): CharSequence {
        if (markdown.isBlank()) return ""
        val parser = MarkdownParser()
        val doc = parser.parse(markdown)
        val builder = SpannableStringBuilder()
        renderNodeToSpannable(doc, builder)
        return builder.toString().trimEnd()
    }

    private fun renderNodeToSpannable(node: MarkdownNode, builder: SpannableStringBuilder) {
        when (node) {
            is MarkdownNode.Document -> node.children.forEach { renderNodeToSpannable(it, builder) }
            is MarkdownNode.Paragraph -> {
                node.content.forEach { renderNodeToSpannable(it, builder) }
                builder.append("\n\n")
            }

            is MarkdownNode.Heading -> {
                val start = builder.length
                node.content.forEach { renderNodeToSpannable(it, builder) }
                val size = when (node.level) {
                    1 -> 2.0f
                    2 -> 1.5f
                    3 -> 1.17f
                    else -> 1.0f
                }
                builder.setSpan(RelativeSizeSpan(size), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("\n\n")
            }

            is MarkdownNode.Text -> builder.append(node.content)
            is MarkdownNode.Bold -> {
                val start = builder.length
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            is MarkdownNode.Italic -> {
                val start = builder.length
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            is MarkdownNode.Strikethrough -> {
                val start = builder.length
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.setSpan(StrikethroughSpan(), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            is MarkdownNode.InlineCode -> {
                val start = builder.length
                builder.append(node.content)
                builder.setSpan(TypefaceSpan("monospace"), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            is MarkdownNode.CodeBlock -> {
                val start = builder.length
                builder.append(node.code)
                builder.setSpan(TypefaceSpan("monospace"), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("\n\n")
            }

            is MarkdownNode.OrderedList -> {
                node.items.forEachIndexed { index, item ->
                    builder.append("${index + node.startNumber}. ")
                    renderNodeToSpannable(item, builder)
                }
            }
            is MarkdownNode.UnorderedList -> {
                node.items.forEach { item ->
                    builder.append("* ")
                    renderNodeToSpannable(item, builder)
                }
            }


            is MarkdownNode.ListItem -> {
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.append("\n")
            }

            is MarkdownNode.TaskListItem -> {
                builder.append(if (node.checked) "☑ " else "☐ ")
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.append("\n")
            }

            else -> {}
        }
    }
}