package com.lightningkite.kiteui.views.l2

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.text.HtmlCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

private class ObservableEditText(context: Context) : androidx.appcompat.widget.AppCompatEditText(context) {
    var onSelectionChangedListener: ((Int, Int) -> Unit)? = null
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
}

actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {
    override val native: LinearLayout = LinearLayout(context.activity).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())

    private val editText = ObservableEditText(context.activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1f
        }
        gravity = android.view.Gravity.TOP
        background = null
        setPadding(16, 16, 16, 16)
        onSelectionChangedListener = { _, _ ->
            updateSelectedRichTextTags()
        }
    }

    init {
        scrollingHorizontally.row {
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER1)
                    ) SelectedSemantic else null
                }
                icon(Icon.header1, "Header 1")
                onClick { this@MarkdownRichTextEditor.applyHeader(1) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER2)
                    ) SelectedSemantic else null
                }
                icon(Icon.header2, "Header 2")
                onClick { this@MarkdownRichTextEditor.applyHeader(2) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER3)
                    ) SelectedSemantic else null
                }
                icon(Icon.header3, "Header 3")
                onClick { this@MarkdownRichTextEditor.applyHeader(3) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.BOLD)
                    ) SelectedSemantic else null
                }
                icon(Icon.bold, "Bold")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ITALIC)
                    ) SelectedSemantic else null
                }
                icon(Icon.italic, "Italic")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.STRIKETHROUGH)
                    ) SelectedSemantic else null
                }
                icon(Icon.strikeThrough, "Strikethrough")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.StrikethroughSpan()) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.QOUTE)
                    ) SelectedSemantic else null
                }
                icon(Icon.qoute, "Quote")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.QuoteSpan()) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.UNORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.unorderedList, "Unordered List")
                onClick { /* Basic list support */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.orderList, "Ordered List")
                onClick { /* Ordered list span */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE)
                    ) SelectedSemantic else null
                }
                icon(Icon.code, "Code")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.TypefaceSpan("monospace")) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE_BLOCK)
                    ) SelectedSemantic else null
                }
                icon(Icon.codeBlock, "Code Block")
                onClick { this@MarkdownRichTextEditor.toggleSpan(android.text.style.TypefaceSpan("monospace")) }
            }
        }

        native.addView(editText)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                (content as Property).update()
                updateSelectedRichTextTags()
            }
        })
    }

    private fun updateSelectedRichTextTags() {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text ?: return
        val tags = mutableSetOf<RichTextTags>()

        // Find spans in the current selection
        val spans = text.getSpans(start.coerceAtMost(end), start.coerceAtLeast(end), Any::class.java)
        for (span in spans) {
            when (span) {
                is android.text.style.StyleSpan -> {
                    if (span.style == android.graphics.Typeface.BOLD) tags.add(RichTextTags.BOLD)
                    if (span.style == android.graphics.Typeface.ITALIC) tags.add(RichTextTags.ITALIC)
                }

                is android.text.style.StrikethroughSpan -> tags.add(RichTextTags.STRIKETHROUGH)
                is android.text.style.UnderlineSpan -> tags.add(RichTextTags.UNDERLINE)
                is android.text.style.QuoteSpan -> tags.add(RichTextTags.QOUTE)
                is android.text.style.RelativeSizeSpan -> {
                    if (span.sizeChange > 1.4f) tags.add(RichTextTags.HEADER1)
                    else if (span.sizeChange > 1.1f) tags.add(RichTextTags.HEADER2)
                }

                is android.text.style.TypefaceSpan -> {
                    if (span.family == "monospace") tags.add(RichTextTags.CODE)
                }
            }
        }

        if (currentSelectedRichTextTags.value != tags) {
            currentSelectedRichTextTags.value = tags
        }
    }

    override fun nativeAddChild(index: Int, element: Element) {
        native.addView(element.native, index)
    }

    override fun nativeRemoveChild(index: Int) {
        native.removeViewAt(index)
    }

    override fun nativeClearChildren() {
        native.removeAllViews()
    }

    private fun applyHeader(level: Int) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val span = android.text.style.RelativeSizeSpan(if (level == 1) 1.5f else 1.2f)
        val flags =
            if (start == end) android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE else android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE

        // Remove existing header spans in this range
        val text = editText.text ?: return
        val existing = text.getSpans(start, end, android.text.style.RelativeSizeSpan::class.java)
        for (s in existing) text.removeSpan(s)

        text.setSpan(span, start, end, flags)
        updateSelectedRichTextTags()
    }

    private fun toggleSpan(span: Any) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text ?: return

        val existing = text.getSpans(start, end, span::class.java).find {
            if (it is android.text.style.StyleSpan && span is android.text.style.StyleSpan) {
                it.style == span.style
            } else if (it is android.text.style.TypefaceSpan && span is android.text.style.TypefaceSpan) {
                it.family == span.family
            } else true
        }

        if (existing != null) {
            text.removeSpan(existing)
        } else {
            val flags =
                if (start == end) android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE else android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            text.setSpan(span, start, end, flags)
        }
        updateSelectedRichTextTags()
    }

    actual var hint: String
        get() = editText.hint?.toString() ?: ""
        set(value) {
            editText.hint = value
        }

    private inner class Property : MutableReactiveValue<String>, BaseListenable() {
        private var _value: String = ""
        override var value: String
            get() = spannableToMarkdown(editText.text ?: android.text.SpannableStringBuilder(""))
            set(v) {
                if (_value != v) {
                    _value = v
                    val html = markdownToHtml(v)
                    editText.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY))
                    invokeAllListeners()
                    updateSelectedRichTextTags()
                }
            }

        fun update() {
            _value = spannableToMarkdown(editText.text ?: android.text.SpannableStringBuilder(""))
            invokeAllListeners()
        }
    }

    actual val content: MutableReactiveValue<String> = Property()

    actual var suggestionHandler: SuggestionHandler? = null

    private fun markdownToHtml(markdown: String): String {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        return HtmlGenerator(markdown, parsedTree, flavour).generateHtml()
    }

    private fun spannableToMarkdown(editable: Editable): String {
        val html = HtmlCompat.toHtml(editable, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // A very basic HTML to Markdown conversion for demonstration.
        // In a real app, a more robust library or converter would be used.
        return html.replace(Regex("<br/?>"), "\n")
            .replace(Regex("<b>|<strong>"), "**")
            .replace(Regex("</b>|</strong>"), "**")
            .replace(Regex("<i>|<em>"), "*")
            .replace(Regex("</i>|</em>"), "*")
            .replace(Regex("<p>"), "")
            .replace(Regex("</p>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }
}
