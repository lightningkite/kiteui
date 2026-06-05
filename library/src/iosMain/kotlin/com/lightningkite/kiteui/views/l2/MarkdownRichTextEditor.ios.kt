package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import kotlinx.cinterop.*
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {
    override val native = UIStackView().apply {
        axis = UILayoutConstraintAxisVertical
        alignment = UIStackViewAlignmentFill
        distribution = UIStackViewDistributionFill
        spacing = 0.0
    }

    private val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())

    private val textView = UITextView().apply {
        backgroundColor = UIColor.clearColor
        delegate = EditorDelegate()
    }

    init {
        bar.row {
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER1)
                    ) SelectedSemantic else null
                }
                icon(Icon.header1, "Header 1")
                onClick { applyStyle(NSFontAttributeName, UIFont.boldSystemFontOfSize(24.0)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER2)
                    ) SelectedSemantic else null
                }
                icon(Icon.header2, "Header 2")
                onClick { applyStyle(NSFontAttributeName, UIFont.boldSystemFontOfSize(20.0)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER3)
                    ) SelectedSemantic else null
                }
                icon(Icon.header3, "Header 3")
                onClick { applyStyle(NSFontAttributeName, UIFont.boldSystemFontOfSize(18.0)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.BOLD)
                    ) SelectedSemantic else null
                }
                icon(Icon.bold, "Bold")
                onClick { applyStyle(NSFontAttributeName, UIFont.boldSystemFontOfSize(UIFont.systemFontSize)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ITALIC)
                    ) SelectedSemantic else null
                }
                icon(Icon.italic, "Italic")
                onClick { applyStyle(NSFontAttributeName, UIFont.italicSystemFontOfSize(UIFont.systemFontSize)) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.STRIKETHROUGH)
                    ) SelectedSemantic else null
                }
                icon(Icon.strikeThrough, "Strikethrough")
                onClick { applyStyle(NSStrikethroughStyleAttributeName, NSUnderlineStyleSingle) }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.QOUTE)
                    ) SelectedSemantic else null
                }
                icon(Icon.qoute, "Quote")
                onClick { /* iOS Quote support */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.UNORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.unorderedList, "Unordered List")
                onClick { /* iOS List support */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.orderList, "Ordered List")
                onClick { /* iOS List support */ }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronRight, "Indent")
                onClick { /* iOS Indent support */ }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronLeft, "Outdent")
                onClick { /* iOS Outdent support */ }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE)
                    ) SelectedSemantic else null
                }
                icon(Icon.code, "Code")
                onClick {
                    applyStyle(
                        NSFontAttributeName,
                        UIFont.fontWithName("Courier", UIFont.systemFontSize)
                            ?: UIFont.systemFontOfSize(UIFont.systemFontSize)
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE_BLOCK)
                    ) SelectedSemantic else null
                }
                icon(Icon.codeBlock, "Code Block")
                onClick {
                    applyStyle(
                        NSFontAttributeName,
                        UIFont.fontWithName("Courier", UIFont.systemFontSize)
                            ?: UIFont.systemFontOfSize(UIFont.systemFontSize)
                    )
                }
            }
        }

        native.addArrangedSubview(textView)

        val delegate = textView.delegate as EditorDelegate
        delegate.onChange = {
            (content as Property).update()
            updateSelectedRichTextTags()
        }
        delegate.onSelectionChange = {
            updateSelectedRichTextTags()
        }
    }

    private fun updateSelectedRichTextTags() {
        val range = textView.selectedRange
        val attrString = textView.attributedText
        val tags = mutableSetOf<RichTextTags>()

        if (range.asDynamic().length > 0) {
            attrString.enumerateAttributesInRange(range, 0u) { attrs, _, _ ->
                if (attrs != null) {
                    detectTagsFromAttributes(attrs, tags)
                }
            }
        } else {
            val attrs = textView.typingAttributes
            detectTagsFromAttributes(attrs as Map<Any?, *>, tags)
        }

        if (currentSelectedRichTextTags.value != tags) {
            currentSelectedRichTextTags.value = tags
        }
    }

    private fun detectTagsFromAttributes(attrs: Map<Any?, *>, tags: MutableSet<RichTextTags>) {
        val font = attrs[NSFontAttributeName] as? UIFont
        if (font != null) {
            val traits = font.fontDescriptor.symbolicTraits
            if ((traits.toInt() and UIFontDescriptorTraitBold.toInt()) != 0) tags.add(RichTextTags.BOLD)
            if ((traits.toInt() and UIFontDescriptorTraitItalic.toInt()) != 0) tags.add(RichTextTags.ITALIC)

            if (font.pointSize > 22.0) tags.add(RichTextTags.HEADER1)
            else if (font.pointSize > 19.0) tags.add(RichTextTags.HEADER2)
            else if (font.pointSize > 17.0 && font.pointSize < 19.0) tags.add(RichTextTags.HEADER3)

            if (font.familyName == "Courier" || font.fontName.contains("Courier", ignoreCase = true)) {
                tags.add(RichTextTags.CODE)
            }
        }
        if (attrs[NSStrikethroughStyleAttributeName] != null && attrs[NSStrikethroughStyleAttributeName] != 0) {
            tags.add(RichTextTags.STRIKETHROUGH)
        }
    }

    override fun nativeAddChild(index: Int, element: Element) {
        native.insertArrangedSubview(element.native as UIView, index.toULong())
    }

    override fun nativeRemoveChild(index: Int) {
        val view = native.arrangedSubviews[index] as UIView
        native.removeArrangedSubview(view)
        view.removeFromSuperview()
    }

    override fun nativeClearChildren() {
        native.arrangedSubviews.forEach {
            val view = it as UIView
            native.removeArrangedSubview(view)
            view.removeFromSuperview()
        }
    }

    private fun applyStyle(name: NSAttributedStringKey, value: Any) {
        val range = textView.selectedRange
        if (range.asDynamic().length > 0) {
            val storage = textView.textStorage
            storage.addAttribute(name, value, range)
        } else {
            val attrs = textView.typingAttributes.toMutableMap()
            attrs[name] = value
            textView.typingAttributes = attrs
        }
        updateSelectedRichTextTags()
    }

    actual var hint: String = ""
        set(value) {
            field = value
        }

    private inner class Property : MutableReactiveValue<String>, BaseListenable() {
        private var _value: String = ""
        override var value: String
            get() = attributedStringToMarkdown(textView.attributedText)
            set(v) {
                if (_value != v) {
                    _value = v
                    val html = markdownToHtml(v)
                    val data = html.toNSData()
                    val options = mapOf(NSDocumentTypeDocumentAttribute to NSHTMLTextDocumentType)
                    textView.attributedText =
                        NSAttributedString(data = data, options = options, documentAttributes = null, error = null)
                    invokeAllListeners()
                    updateSelectedRichTextTags()
                }
            }

        fun update() {
            _value = attributedStringToMarkdown(textView.attributedText)
            invokeAllListeners()
        }
    }

    actual val content: MutableReactiveValue<String> = Property()

    actual var suggestionHandler: SuggestionHandler? = null

    actual override var action: Action? = null

    private fun markdownToHtml(markdown: String): String {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        return HtmlGenerator(markdown, parsedTree, flavour).generateHtml()
    }

    private fun attributedStringToMarkdown(attrString: NSAttributedString): String {
        val html = attrString.toHtml()
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

private class EditorDelegate : NSObject(), UITextViewDelegateProtocol {
    var onChange: (() -> Unit)? = null
    var onSelectionChange: (() -> Unit)? = null
    override fun textViewDidChange(textView: UITextView) {
        onChange?.invoke()
    }

    override fun textViewDidChangeSelection(textView: UITextView) {
        onSelectionChange?.invoke()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toNSData(): NSData = (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!

@OptIn(ExperimentalForeignApi::class)
private fun NSAttributedString.toHtml(): String {
    val data = dataFromRange(
        range = NSMakeRange(0u, length),
        documentAttributes = mapOf(NSDocumentTypeDocumentAttribute to NSHTMLTextDocumentType),
        error = null
    ) ?: return ""
    return NSString(data = data, encoding = NSUTF8StringEncoding).toString()
}
