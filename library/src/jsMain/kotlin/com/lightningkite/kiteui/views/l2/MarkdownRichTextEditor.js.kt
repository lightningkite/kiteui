package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.l2.editorHelpers.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.parsing.DOMParser
import kotlin.coroutines.CoroutineContext
import kotlin.js.json


@OptIn(UnsafeModifier::class)
actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {
    internal val ZERO_WIDTH_SPACE = "\u200B"
    internal val FORMATTING_TAGS =
        listOf("STRONG", "EM", "B", "I", "CODE", "H1", "H2", "H3", "BLOCKQUOTE", "PRE", "S", "DEL", "U")
    internal val BLOCK_ELEMENTS = listOf("P", "H1", "H2", "H3", "LI", "BLOCKQUOTE", "PRE")
    internal val DELETE_INPUT_TYPES = listOf("deleteContentBackward", "deleteContentForward", "deleteByCut")
    internal val EMPTY_FORMATTING_TAGS =
        listOf("STRONG", "EM", "B", "I", "CODE", "H1", "H2", "H3", "BLOCKQUOTE", "PRE", "S", "DEL", "U")

    internal var isProcessingEnter = false

    var cursorIndex: Pair<Int, Int>? = null

    var currentTrigger: String? = null
    var currentQuery: String = ""

    val isOrderedList = Signal(false)
    val _listOnly = Signal(false)

    var suggestionPopup: Element? = null
    var suggestionStartPosition: Int = 0
    var currentSuggestions: List<String> = emptyList()
    var selectedSuggestionIndex: Int = 0
    var isSuggestionActive: Boolean = false
    var documentClickListener: ((Event) -> Unit)? = null

    var linkToolbarButton: com.lightningkite.kiteui.views.Element? = null

    var isListeningForSuggestions = false

    val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())

    fun modifyIndent(increase: Boolean) {
        val doc = window.document.asDynamic()
        if (increase) {
            doc.execCommand("indent", false, null)
        } else {
            doc.execCommand("outdent", false, null)
        }
        notifyContentChanged()
    }

    val htmlToMarkdownProcessor = TurndownService(
        json(
            "headingStyle" to "atx",
            "emDelimiter" to "*",
            "codeBlockStyle" to "fenced"
        ).unsafeCast<TurndownOptions>()
    ).also { svc ->
        svc.keep(arrayOf("u", "sup", "sub", "br"))
        val strikethroughRule: dynamic = js("{}")
        strikethroughRule.filter = arrayOf("s", "del")
        strikethroughRule.replacement = { content: String, _: dynamic, _: dynamic -> "~~$content~~" }
        svc.addRule("strikethrough", strikethroughRule)
        val preRule: dynamic = js("{}")
        preRule.filter = arrayOf("pre")
        preRule.replacement = { _: String, node: dynamic, _: dynamic ->
            val element = node as org.w3c.dom.HTMLElement

            // 1. Try innerText first, as it naturally respects <p> and <br> newlines.
            var rawText = element.asDynamic().innerText as? String

            // 2. Fallback if innerText is unavailable in a detached DOM state
            if (rawText.isNullOrEmpty() || rawText == "undefined") {
                val htmlContent = element.innerHTML
                rawText = htmlContent
                    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("</p>\\s*<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("</p>|<p[^>]*>|</div>|<div[^>]*>"), "\n")
                    .replace(Regex("<[^>]*>"), "") // Strip all remaining tags
                    .replace("&nbsp;", " ")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
            }

            "\n```\n${rawText.trimEnd()}\n```\n"
        }
        svc.addRule("forceFencedPre", preRule)
    }

    init {
        native.tag = "div"
        native.setAttribute("markdown-container", "true")
        native.setStyleProperty("display", "flex")
        native.setStyleProperty("flex-direction", "column")
        native.setStyleProperty("align-items", "stretch")

        // Forward background clicks to the editable text area
        native.addEventListener("click") { event ->
            // If the user clicked the empty background of the container itself
            if (event.target == native) {
                val el = textArea.element as? HTMLElement
                el?.focus()
            }
        }
        scrollingHorizontally.row {
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke().contains(RichTextTags.HEADER1))
                        SelectedSemantic else null
                }
                icon(Icon.header1, "Header 1")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("h1")
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER2)
                    ) SelectedSemantic else null
                }
                icon(Icon.header2, "Header 2")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("h2")
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.HEADER3)
                    ) SelectedSemantic else null
                }
                icon(Icon.header3, "Header 3")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("h3")
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.BOLD)
                    ) SelectedSemantic else null
                }
                icon(Icon.bold, "Bold")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("strong")
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ITALIC)
                    ) SelectedSemantic else null
                }
                icon(Icon.italic, "Italic")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("em")
                }
            }

            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.STRIKETHROUGH)
                    ) SelectedSemantic else null
                }
                icon(Icon.strikeThrough, "strikethrough")
                onClick { this@MarkdownRichTextEditor.insertHtmlElement("s") }
            }

            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.QOUTE)
                    ) SelectedSemantic else null
                }
                icon(Icon.qoute, "Quote")
                onClick { this@MarkdownRichTextEditor.insertHtmlElement("blockquote") }
            }

            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.UNORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.unorderedList, "Unordered List")
                onClick {
                    this@MarkdownRichTextEditor.insertList(ordered = false)
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.ORDERED_LIST)
                    ) SelectedSemantic else null
                }
                icon(Icon.orderList, "Ordered List")
                onClick {
                    this@MarkdownRichTextEditor.insertList(ordered = true)
                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme { null }
                icon(Icon.chevronRight, "Indent")
                onClick { this@MarkdownRichTextEditor.modifyIndent(true) }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme { null }
                icon(Icon.chevronLeft, "Outdent")
                onClick { this@MarkdownRichTextEditor.modifyIndent(false) }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE)
                    ) SelectedSemantic else null
                }
                icon(Icon.code, "Code")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("code")

                }
            }
            button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.CODE_BLOCK)
                    ) SelectedSemantic else null
                }
                icon(Icon.codeBlock, "Code Block")
                onClick {
                    this@MarkdownRichTextEditor.insertHtmlElement("pre")

                }
            }
            this@MarkdownRichTextEditor.linkToolbarButton = (button {
                native.addEventListener("mousedown") { it.preventDefault() }
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags()
                            .contains(RichTextTags.LINK)
                    ) SelectedSemantic else null
                }
                icon(Icon.link, "Insert Link")
                onClick {
                    this@MarkdownRichTextEditor.insertLink()
                }
            } as com.lightningkite.kiteui.views.Element)
        }
    }


    private var invokeContentListeners: () -> Unit = {}
    internal fun notifyContentChanged() = invokeContentListeners()

    internal var savedSelectionRange: dynamic = null

    val textArea = FutureElement().apply {
        tag = "div"
        setAttribute("contenteditable", "true")
        setStyleProperty("min-height", "3rem")
        setStyleProperty("outline", "none")
        // Let the text area expand and occupy all remaining vertical space inside the 50rem box
        setStyleProperty("flex-grow", "1")
        setStyleProperty("overflow-y", "auto")
        native.appendChild(this)
    }
    actual var hint: String = ""
        set(value) {
            field = value
            textArea.setAttribute("data-placeholder", value)
        }

    var lastEnterInPreTime: Double = 0.0

    data class ListContext(
        val listItem: HTMLElement,
        val listElement: HTMLElement,
        val formattingElements: List<HTMLElement>,
        val selection: dynamic
    )

    actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String>, BaseListenable() {
        init {
            invokeContentListeners = { invokeAllListeners() }

            textArea.addEventListener("beforeinput") { event ->
                beforeInput(event)
                return@addEventListener
            }

            textArea.addEventListener("input") {
                handleInput(it)
                invokeAllListeners()
            }
            textArea.addEventListener("click") {
                handleClick(it)
            }

            textArea.addEventListener("keyup") { event ->
                handleKeyUp(event)
            }

            textArea.addEventListener("blur") {
                val sel = window.asDynamic().getSelection()
                if (sel != null && sel.rangeCount > 0) {
                    savedSelectionRange = sel.getRangeAt(0).cloneRange()
                }
                launch {
                    currentSelectedRichTextTags.set(emptySet())
                }
            }

            textArea.addEventListener("focus") {
                launch {
                    updateSelectedRichTextTag()
                }
            }

            textArea.addEventListener("keydown") { event ->
                handleKeyDown(event)
            }

            textArea.addEventListener("paste") { event ->
                event.preventDefault()
                val clipboardData = event.asDynamic().clipboardData ?: window.asDynamic().clipboardData
                val pastedText = clipboardData.getData("text/plain") as? String ?: return@addEventListener

                val selection = window.asDynamic().getSelection()
                if (selection != null && selection.rangeCount > 0) {
                    val range = selection.getRangeAt(0)
                    range.deleteContents()

                    val lines = pastedText.split(Regex("\\r?\\n"))
                    val fragment = document.createDocumentFragment()
                    var lastNode: Node? = null

                    lines.forEachIndexed { index, line ->
                        // If the line is empty, inject a Non-Breaking Space (\u00A0)
                        // This forces Markdown to respect the empty line and not collapse it on save.
                        val textContent = if (line.isEmpty()) "\u00A0" else line
                        val textNode = document.createTextNode(textContent)

                        fragment.appendChild(textNode)
                        lastNode = textNode

                        if (index < lines.lastIndex) {
                            val br = document.createElement("br")
                            fragment.appendChild(br)
                            lastNode = br
                        }
                    }

                    range.insertNode(fragment)

                    if (lastNode != null) {
                        range.setStartAfter(lastNode!!)
                        range.collapse(true)
                    } else {
                        range.collapse(false)
                    }

                    selection.removeAllRanges()
                    selection.addRange(range)
                }
                invokeAllListeners()
            }

            document.addEventListener("selectionchange", { _ ->
                val selection = window.asDynamic().getSelection()
                if (selection != null && selection.rangeCount > 0) {
                    val range = selection.getRangeAt(0)
                    val startContainer = range.startContainer
                    val endContainer = range.endContainer

                    if (isNodeInsideEditor(startContainer, textArea.element as HTMLElement) ||
                        isNodeInsideEditor(endContainer, textArea.element as HTMLElement)
                    ) {
                        launch {
                            updateSelectedRichTextTag()
                        }
                    }
                }
            })
        }

        override var value: String
            get() {
                val rawHtml = textArea.element?.innerHTML ?: textArea.innerHtmlUnsafe ?: ""
                // Strip out all zero-width spaces before Markdown conversion
                val cleanedHtml = rawHtml.replace("\u200B", "")

                var md = htmlToMarkdownProcessor.turndown(cleanedHtml)
                return md
            }
            set(value) {
                println("DEBUG set value ${value}")

                fun String.normalizeForComparison(): String {
                    return this.replace("\u200B", "")
                        .replace(Regex("""^[-*+]\s+""", RegexOption.MULTILINE), "* ")
                        .replace(Regex("""^\d+\.\s+""", RegexOption.MULTILINE), "1. ")
                        .trimEnd()
                }

                if (this.value.normalizeForComparison() == value.normalizeForComparison()) {
                    return
                }

                val isFocused = document.activeElement == textArea.element
                val targetOffset = if (isFocused) getCleanCursorOffset() else 0
                if (value.isBlank()) {
                    textArea.innerHtmlUnsafe = ""
                    if (isFocused) restoreCursorPosition(targetOffset)
                    return
                }
                val f = GFMFlavourDescriptor()
                val html = MarkdownParser(f).buildMarkdownTreeFromString(value).let {
                    HtmlGenerator(value, it, f, false).generateHtml()
                }.also { println("Actual content: $it") }

                var cleanHtml = html
                    .removePrefix("<body>")
                    .removeSuffix("</body>")
                    .trim().also { println("Actual cleaned content: $it") }

                textArea.element?.let { element ->
                    val parser = DOMParser()
                    val doc = parser.parseFromString(cleanHtml, "text/html")

                    // 1. Clear existing content safely
                    while (element.firstChild != null) {
                        element.removeChild(element.firstChild!!)
                    }

                    // 2. Safely transfer the parsed native W3C nodes into the editor
                    val body = doc.body
                    while (body != null && body.firstChild != null) {
                        val child = body.firstChild!!
                        element.appendChild(child)
                    }
                } ?: run {
                    textArea.innerHtmlUnsafe = cleanHtml
                }

                if (isFocused) {
                    restoreCleanCursorOffset(targetOffset)
                }
                launch {
                    updateSelectedRichTextTag()
                }
            }
    }

    actual var suggestionHandler: SuggestionHandler? = null
}
