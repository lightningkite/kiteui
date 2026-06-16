package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.markdown.MarkdownNode
import com.lightningkite.kiteui.markdown.MarkdownParser
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(com.lightningkite.kiteui.UnsafeModifier::class)
actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {

    override val native = UIStackView().apply {
        axis = UILayoutConstraintAxisVertical
        alignment = UIStackViewAlignmentFill
        distribution = UIStackViewDistributionFill
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        nativeTextView.textColor = theme.theme.foreground.closestColor().toUiColor()
        // iOS UITextView doesn't have a built-in hint property like EditText,
        // typically this requires a background label or custom drawing,
        // but styling the text itself uses the theme here.
    }

    private var recursive = false
    val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())
    actual var suggestionHandler: SuggestionHandler? = null

    // Native iOS UITextView
    val nativeTextView = UITextView().apply {
        font = UIFont.systemFontOfSize(UIFont.systemFontSize)
        allowsEditingTextAttributes = true
        backgroundColor = UIColor.clearColor

        // Swipe gestures for indent/outdent
        val swipeRight =
            UISwipeGestureRecognizer(target = this@MarkdownRichTextEditor, action = NSSelectorFromString("swipeRight:"))
        swipeRight.direction = UISwipeGestureRecognizerDirectionRight
        addGestureRecognizer(swipeRight)

        val swipeLeft =
            UISwipeGestureRecognizer(target = this@MarkdownRichTextEditor, action = NSSelectorFromString("swipeLeft:"))
        swipeLeft.direction = UISwipeGestureRecognizerDirectionLeft
        addGestureRecognizer(swipeLeft)
    }

    @ObjCAction
    fun swipeRight(sender: UISwipeGestureRecognizer) {
        modifyIndent(true)
    }

    @ObjCAction
    fun swipeLeft(sender: UISwipeGestureRecognizer) {
        modifyIndent(false)
    }

    private fun modifyIndent(increase: Boolean) {
        val selectedRange = nativeTextView.selectedRange
        val text = nativeTextView.text ?: return
        val nsText = text as NSString

        // Find line start
        val lineStart = if (selectedRange.location > 0u) {
            val range =
                nsText.rangeOfString("\n", options = NSBackwardsSearch, range = NSMakeRange(0u, selectedRange.location))
            if (range.location == NSNotFound.toULong()) 0u else range.location + 1u
        } else 0u

        if (increase) {
            val newText = nsText.stringByReplacingCharactersInRange(NSMakeRange(lineStart, 0u), withString = "    ")
            nativeTextView.text = newText
            nativeTextView.selectedRange = NSMakeRange(selectedRange.location + 4u, selectedRange.length)
        } else {
            val linePrefix = nsText.substringWithRange(NSMakeRange(lineStart, minOf(4u, nsText.length - lineStart)))
            if (linePrefix.startsWith("    ")) {
                val newText = nsText.stringByReplacingCharactersInRange(NSMakeRange(lineStart, 4u), withString = "")
                nativeTextView.text = newText
                nativeTextView.selectedRange = NSMakeRange(maxOf(0u, selectedRange.location - 4u), selectedRange.length)
            }
        }
        (content as? ContentValue)?.update()
    }

    private val delegate = object : NSObject(), UITextViewDelegateProtocol {
        override fun textViewDidChange(textView: UITextView) {
            if (recursive) return
            updateSelectedTags()
            (content as? ContentValue)?.update()
        }

        override fun textViewDidChangeSelection(textView: UITextView) {
            if (recursive) return
            updateSelectedTags()
        }

        override fun textView(
            textView: UITextView,
            shouldChangeTextInRange: CValue<NSRange>,
            replacementText: String
        ): Boolean {
            if (recursive) return true

            // Handle automatic list generation on newline
            if (replacementText == "\n") {
                val nsText = textView.text as NSString
                val location = shouldChangeTextInRange.useContents { location }

                val lineStart = if (location > 0u) {
                    val range =
                        nsText.rangeOfString("\n", options = NSBackwardsSearch, range = NSMakeRange(0u, location))
                    if (range.location == NSNotFound.toULong()) 0u else range.location + 1u
                } else 0u

                val lineText = nsText.substringWithRange(NSMakeRange(lineStart, location - lineStart))
                val trimmed = lineText.trimStart()
                val indentLength = lineText.length - trimmed.length
                val indent = lineText.take(indentLength)

                if (trimmed.startsWith("* ")) {
                    recursive = true
                    if (trimmed == "* ") {
                        textView.text = nsText.stringByReplacingCharactersInRange(
                            NSMakeRange(lineStart, location - lineStart),
                            withString = ""
                        )
                    } else {
                        val insert = "\n$indent* "
                        textView.text =
                            nsText.stringByReplacingCharactersInRange(shouldChangeTextInRange, withString = insert)
                        textView.selectedRange = NSMakeRange(location + insert.length.toULong(), 0u)
                    }
                    recursive = false
                    return false
                } else if (trimmed.matches(Regex("^\\d+\\.\\s+.*"))) {
                    val match = Regex("^(\\d+)\\.\\s+").find(trimmed)
                    if (match != null) {
                        recursive = true
                        val num = match.groupValues[1].toInt()
                        if (trimmed.length <= match.value.length) {
                            textView.text = nsText.stringByReplacingCharactersInRange(
                                NSMakeRange(lineStart, location - lineStart),
                                withString = ""
                            )
                        } else {
                            val insert = "\n$indent${num + 1}. "
                            textView.text =
                                nsText.stringByReplacingCharactersInRange(shouldChangeTextInRange, withString = insert)
                            textView.selectedRange = NSMakeRange(location + insert.length.toULong(), 0u)
                        }
                        recursive = false
                        return false
                    }
                }
            }
            return true
        }
    }

    init {
        nativeTextView.delegate = delegate

        scrollingHorizontally.row {
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.HEADER1)) SelectedSemantic else null
                }
                icon(Icon.header1, "Header 1")
                onClick {
                    toggleAttribute(
                        NSFontAttributeName,
                        UIFont.boldSystemFontOfSize(UIFont.systemFontSize * 2.0),
                        RichTextTags.HEADER1
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.HEADER2)) SelectedSemantic else null
                }
                icon(Icon.header2, "Header 2")
                onClick {
                    toggleAttribute(
                        NSFontAttributeName,
                        UIFont.boldSystemFontOfSize(UIFont.systemFontSize * 1.5),
                        RichTextTags.HEADER2
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.HEADER3)) SelectedSemantic else null
                }
                icon(Icon.header3, "Header 3")
                onClick {
                    toggleAttribute(
                        NSFontAttributeName,
                        UIFont.boldSystemFontOfSize(UIFont.systemFontSize * 1.17),
                        RichTextTags.HEADER3
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.BOLD)) SelectedSemantic else null
                }
                icon(Icon.bold, "Bold")
                onClick { toggleFontTrait(UIFontDescriptorTraitBold, RichTextTags.BOLD) }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.ITALIC)) SelectedSemantic else null
                }
                icon(Icon.italic, "Italic")
                onClick { toggleFontTrait(UIFontDescriptorTraitItalic, RichTextTags.ITALIC) }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.STRIKETHROUGH)
                    ) SelectedSemantic else null
                }
                icon(Icon.strikeThrough, "Strikethrough")
                onClick {
                    toggleAttribute(
                        NSStrikethroughStyleAttributeName,
                        NSUnderlineStyleSingle,
                        RichTextTags.STRIKETHROUGH
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.QOUTE)) SelectedSemantic else null
                }
                icon(Icon.qoute, "Quote")
                onClick { /* Implement Quote Logic */ }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.unorderedList, "Unordered List")
                onClick { insertList(ordered = false) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.orderList, "Ordered List")
                onClick { insertList(ordered = true) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronRight, "Indent")
                onClick { modifyIndent(true) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronLeft, "Outdent")
                onClick { modifyIndent(false) }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke().contains(RichTextTags.CODE)) SelectedSemantic else null
                }
                icon(Icon.code, "Code")
                onClick {
                    toggleAttribute(
                        NSFontAttributeName,
                        UIFont.monospacedSystemFontOfSize(UIFont.systemFontSize, UIFontWeightRegular),
                        RichTextTags.CODE
                    )
                }
            }
            button {
                applyDynamicTheme {
                    if (currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.CODE_BLOCK)
                    ) SelectedSemantic else null
                }
                icon(Icon.codeBlock, "Code Block")
                onClick { /* Implement Code Block Logic */ }
            }
        }

        addChild(object : NativeElement(context) {
            override val native = this@MarkdownRichTextEditor.nativeTextView
        })
    }

    actual var hint: String = ""
    // Note: iOS UITextView doesn't support hint natively. Handled via placeholder labels in custom implementations.

    actual val content: MutableReactiveValue<String> = ContentValue()

    inner class ContentValue : MutableReactiveValue<String>, BaseListenable() {
        fun update() = invokeAllListeners()
        override var value: String
            get() = serializeToMarkdown(nativeTextView.attributedText)
            set(value) {
                if (serializeToMarkdown(nativeTextView.attributedText) != value) {
                    val isFocused = nativeTextView.isFirstResponder
                    val selectedRange = nativeTextView.selectedRange

                    recursive = true
                    nativeTextView.attributedText = parseMarkdownToAttributedString(value)
                    recursive = false

                    if (isFocused) {
                        nativeTextView.selectedRange = selectedRange
                    }
                    updateSelectedTags()
                }
            }
    }

    // --- Formatting Toggles ---

    private fun toggleFontTrait(trait: UInt, tag: RichTextTags) {
        val range = nativeTextView.selectedRange
        if (range.useContents { length == 0uL }) return // Simplified for non-selection typing

        val attributedText = NSMutableAttributedString.create(attributedString = nativeTextView.attributedText)
        var hasTrait = false

        attributedText.enumerateAttribute(NSFontAttributeName, inRange = range, options = 0u) { value, attrRange, _ ->
            val font = value as? UIFont ?: return@enumerateAttribute
            val traits = font.fontDescriptor.symbolicTraits
            if ((traits and trait) == trait) {
                hasTrait = true
            }
        }

        attributedText.enumerateAttribute(NSFontAttributeName, inRange = range, options = 0u) { value, attrRange, _ ->
            val font = value as? UIFont ?: UIFont.systemFontOfSize(UIFont.systemFontSize)
            var newTraits = font.fontDescriptor.symbolicTraits

            newTraits = if (hasTrait) {
                newTraits and trait.inv()
            } else {
                newTraits or trait
            }

            val newDescriptor = font.fontDescriptor.fontDescriptorWithSymbolicTraits(newTraits)
            val newFont = newDescriptor?.let { UIFont.fontWithDescriptor(it, 0.0) } ?: font
            attributedText.addAttribute(NSFontAttributeName, newFont, range = attrRange)
        }

        nativeTextView.attributedText = attributedText
        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    private fun toggleAttribute(attrName: String, value: Any, tag: RichTextTags) {
        val range = nativeTextView.selectedRange
        val attributedText = NSMutableAttributedString.create(attributedString = nativeTextView.attributedText)

        var hasAttr = false
        attributedText.enumerateAttribute(attrName, inRange = range, options = 0u) { existingValue, _, _ ->
            if (existingValue != null) hasAttr = true
        }

        if (hasAttr) {
            attributedText.removeAttribute(attrName, range)
        } else {
            attributedText.addAttribute(attrName, value, range)
        }

        nativeTextView.attributedText = attributedText
        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    private fun insertList(ordered: Boolean) {
        val range = nativeTextView.selectedRange
        val nsText = nativeTextView.text as NSString
        val lineStart = if (range.location > 0u) {
            val searchRange =
                nsText.rangeOfString("\n", options = NSBackwardsSearch, range = NSMakeRange(0u, range.location))
            if (searchRange.location == NSNotFound.toULong()) 0u else searchRange.location + 1u
        } else 0u

        val insertText = if (ordered) "1. " else "* "
        val newText = nsText.stringByReplacingCharactersInRange(NSMakeRange(lineStart, 0u), withString = insertText)
        nativeTextView.text = newText
        nativeTextView.selectedRange = NSMakeRange(range.location + insertText.length.toULong(), range.length)

        updateSelectedTags()
        (content as? ContentValue)?.update()
    }

    private fun updateSelectedTags() {
        val range = nativeTextView.selectedRange
        val tags = mutableSetOf<RichTextTags>()
        if (nativeTextView.attributedText.length == 0uL) return

        val checkRange = if (range.length > 0uL) range else NSMakeRange(maxOf(0u, range.location - 1u), 1u)

        nativeTextView.attributedText.enumerateAttributesInRange(checkRange, options = 0u) { attrs, _, _ ->
            if (attrs == null) return@enumerateAttributesInRange

            val font = attrs[NSFontAttributeName] as? UIFont
            if (font != null) {
                val traits = font.fontDescriptor.symbolicTraits
                if ((traits and UIFontDescriptorTraitBold) != 0u) tags.add(RichTextTags.BOLD)
                if ((traits and UIFontDescriptorTraitItalic) != 0u) tags.add(RichTextTags.ITALIC)

                if (font.fontName.contains("Courier") || font.fontName.contains("Mono")) tags.add(RichTextTags.CODE)

                val pointSize = font.pointSize
                val baseSize = UIFont.systemFontSize
                if (pointSize >= baseSize * 2.0) tags.add(RichTextTags.HEADER1)
                else if (pointSize >= baseSize * 1.5) tags.add(RichTextTags.HEADER2)
                else if (pointSize >= baseSize * 1.17) tags.add(RichTextTags.HEADER3)
            }

            if (attrs[NSStrikethroughStyleAttributeName] != null) {
                tags.add(RichTextTags.STRIKETHROUGH)
            }
        }

        if (currentSelectedRichTextTags.value != tags) {
            currentSelectedRichTextTags.value = tags
        }
    }

    // --- Markdown Conversion ---

    private fun serializeToMarkdown(attributedString: NSAttributedString?): String {
        if (attributedString == null || attributedString.length == 0uL) return ""
        val result = StringBuilder()
        val plainText = attributedString.string

        attributedString.enumerateAttributesInRange(
            NSMakeRange(0u, attributedString.length),
            options = 0u
        ) { attrs, range, _ ->
            if (attrs == null) return@enumerateAttributesInRange
            val substring = (plainText as NSString).substringWithRange(range)

            var prefix = ""
            var suffix = ""

            val font = attrs[NSFontAttributeName] as? UIFont
            if (font != null) {
                val traits = font.fontDescriptor.symbolicTraits
                if ((traits and UIFontDescriptorTraitBold) != 0u) {
                    prefix += "**"; suffix = "**$suffix"
                }
                if ((traits and UIFontDescriptorTraitItalic) != 0u) {
                    prefix += "*"; suffix = "*$suffix"
                }
                if (font.fontName.contains("Courier") || font.fontName.contains("Mono")) {
                    prefix += "`"; suffix = "`$suffix"
                }

                // Headings (basic approximation)
                val pointSize = font.pointSize
                if (pointSize >= UIFont.systemFontSize * 2.0) prefix = "# " + prefix
                else if (pointSize >= UIFont.systemFontSize * 1.5) prefix = "## " + prefix
                else if (pointSize >= UIFont.systemFontSize * 1.17) prefix = "### " + prefix
            }

            if (attrs[NSStrikethroughStyleAttributeName] != null) {
                prefix += "~~"
                suffix = "~~$suffix"
            }

            result.append(prefix).append(substring).append(suffix)
        }

        return result.toString()
    }

    private fun parseMarkdownToAttributedString(markdown: String): NSAttributedString {
        if (markdown.isBlank()) return NSAttributedString.create(string = "")
        val parser = MarkdownParser()
        val doc = parser.parse(markdown)
        val builder = NSMutableAttributedString.create(string = "")
        renderNodeToAttributedString(doc, builder)
        return builder
    }

    private fun renderNodeToAttributedString(node: MarkdownNode, builder: NSMutableAttributedString) {
        when (node) {
            is MarkdownNode.Document -> node.children.forEach { renderNodeToAttributedString(it, builder) }
            is MarkdownNode.Paragraph -> {
                node.content.forEach { renderNodeToAttributedString(it, builder) }
                builder.appendAttributedString(NSAttributedString.create(string = "\n\n"))
            }

            is MarkdownNode.Heading -> {
                val start = builder.length
                node.content.forEach { renderNodeToAttributedString(it, builder) }
                val size = when (node.level) {
                    1 -> 2.0
                    2 -> 1.5
                    3 -> 1.17
                    else -> 1.0
                }
                val font = UIFont.boldSystemFontOfSize(UIFont.systemFontSize * size)
                builder.addAttribute(NSFontAttributeName, font, NSMakeRange(start, builder.length - start))
                builder.appendAttributedString(NSAttributedString.create(string = "\n\n"))
            }

            is MarkdownNode.Text -> builder.appendAttributedString(NSAttributedString.create(string = node.content))
            is MarkdownNode.Bold -> {
                val start = builder.length
                node.children.forEach { renderNodeToAttributedString(it, builder) }
                // Note: Simplified. Proper trait addition requires reading existing font and appending traits.
                val font = UIFont.boldSystemFontOfSize(UIFont.systemFontSize)
                builder.addAttribute(NSFontAttributeName, font, NSMakeRange(start, builder.length - start))
            }

            is MarkdownNode.Italic -> {
                val start = builder.length
                node.children.forEach { renderNodeToAttributedString(it, builder) }
                val font = UIFont.italicSystemFontOfSize(UIFont.systemFontSize)
                builder.addAttribute(NSFontAttributeName, font, NSMakeRange(start, builder.length - start))
            }

            is MarkdownNode.Strikethrough -> {
                val start = builder.length
                node.children.forEach { renderNodeToAttributedString(it, builder) }
                builder.addAttribute(
                    NSStrikethroughStyleAttributeName,
                    NSUnderlineStyleSingle,
                    NSMakeRange(start, builder.length - start)
                )
            }

            is MarkdownNode.InlineCode -> {
                val start = builder.length
                builder.appendAttributedString(NSAttributedString.create(string = node.content))
                val font = UIFont.monospacedSystemFontOfSize(UIFont.systemFontSize, UIFontWeightRegular)
                builder.addAttribute(NSFontAttributeName, font, NSMakeRange(start, builder.length - start))
            }

            is MarkdownNode.CodeBlock -> {
                val start = builder.length
                builder.appendAttributedString(NSAttributedString.create(string = node.code))
                val font = UIFont.monospacedSystemFontOfSize(UIFont.systemFontSize, UIFontWeightRegular)
                builder.addAttribute(NSFontAttributeName, font, NSMakeRange(start, builder.length - start))
                builder.appendAttributedString(NSAttributedString.create(string = "\n\n"))
            }

            is MarkdownNode.OrderedList -> {
                node.items.forEachIndexed { index, item ->
                    builder.appendAttributedString(NSAttributedString.create(string = "${index + node.startNumber}. "))
                    renderNodeToAttributedString(item, builder)
                }
            }

            is MarkdownNode.UnorderedList -> {
                node.items.forEach { item ->
                    builder.appendAttributedString(NSAttributedString.create(string = "* "))
                    renderNodeToAttributedString(item, builder)
                }
            }

            is MarkdownNode.ListItem -> {
                node.children.forEach { renderNodeToAttributedString(it, builder) }
                builder.appendAttributedString(NSAttributedString.create(string = "\n"))
            }

            else -> {}
        }
    }
}