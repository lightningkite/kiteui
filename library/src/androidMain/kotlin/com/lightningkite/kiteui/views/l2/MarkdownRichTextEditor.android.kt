package com.lightningkite.kiteui.views.l2

import android.graphics.Paint
import android.graphics.Rect
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
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lightningkite.kiteui.views.direct.SlightlyModifiedLinearLayout
import com.lightningkite.kiteui.views.direct.SimplifiedLinearLayout
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.SelectedSemantic

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.icon
import com.lightningkite.kiteui.markdown.MarkdownNode
import com.lightningkite.kiteui.markdown.MarkdownParser
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.applyAlpha
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch

@OptIn(com.lightningkite.kiteui.UnsafeModifier::class)
actual class MarkdownRichTextEditor actual constructor(context: ElementContext) :
    NativeContainerElementWithAction(context) {

    override val native = SlightlyModifiedLinearLayout(context.activity).apply {
        orientation = SimplifiedLinearLayout.VERTICAL
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        nativeEditText.setTextColor(theme.theme.foreground.colorInt())
        // Apply a semitransparent foreground color to the hint
        nativeEditText.setHintTextColor(theme.theme.foreground.applyAlpha(0.5f).colorInt())

    }




    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

    private var recursive = false

    val currentSelectedRichTextTags = Signal<Set<RichTextTags>>(emptySet())

    var linkToolbarButton: Element? = null

    actual var suggestionHandler: SuggestionHandler? = null

    private fun modifyIndent(increase: Boolean) {
        val start = nativeEditText.selectionStart
        val spannable = nativeEditText.text ?: return
        val currentLineStart = if (start > 0) {
            val lastNewline = spannable.lastIndexOf('\n', start - 1)
            if (lastNewline == -1) 0 else lastNewline + 1
        } else 0

        if (increase) {
            spannable.insert(currentLineStart, "    ")
        } else {
            val lineText = spannable.substring(currentLineStart, spannable.length)
            if (lineText.startsWith("    ")) {
                spannable.delete(currentLineStart, currentLineStart + 4)
            }
        }
        (content as? ContentValue)?.update()
    }

    // Native Android EditText to handle the typing and spans
    val nativeEditText = EditText(context.activity).apply {
        layoutParams = SimplifiedLinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        background = null // Remove default underline
        gravity = Gravity.TOP or Gravity.START
        minLines = 3

        // Ensure it takes focus and shows keyboard
        isFocusable = true
        isFocusableInTouchMode = true

        val gestureDetector = GestureDetector(context.activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100) {
                    if (deltaX > 0) { // Swipe Right - Indent
                        modifyIndent(true)
                        return true
                    } else { // Swipe Left - Outdent
                        modifyIndent(false)
                        return true
                    }
                }
                return false
            }
        })

        setOnTouchListener { v, event ->
            val result = gestureDetector.onTouchEvent(event)
            if (result) return@setOnTouchListener true
            v.onTouchEvent(event)
        }
    }


    init {
        addChild(object : NativeElement(context) {
            override val native = this@MarkdownRichTextEditor.nativeEditText
        })

        themed(ThemeDerivation { it.withBack }).scrollingHorizontally.row {
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
                applyDynamicTheme { null }
                icon(Icon.unorderedList, "Unordered List")
                onClick { this@MarkdownRichTextEditor.insertList(ordered = false) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.orderList, "Ordered List")
                onClick { this@MarkdownRichTextEditor.insertList(ordered = true) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronRight, "Indent")
                onClick { this@MarkdownRichTextEditor.modifyIndent(true) }
            }
            button {
                applyDynamicTheme { null }
                icon(Icon.chevronLeft, "Outdent")
                onClick { this@MarkdownRichTextEditor.modifyIndent(false) }
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
            this@MarkdownRichTextEditor.linkToolbarButton = (button {
                applyDynamicTheme {
                    if (this@MarkdownRichTextEditor.currentSelectedRichTextTags.invoke()
                            .contains(RichTextTags.LINK)
                    ) SelectedSemantic else null
                }
                icon(Icon.link, "Insert Link")
                onClick { this@MarkdownRichTextEditor.insertLink() }
            } as Element)
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

        val toolbarView = native.getChildAt(native.childCount - 1)

        // 3. Toolbar position: at its natural layout position (bottom of editor) normally,
        //    sticky above the keyboard when the keyboard overlaps the editor.
        //    Only translations are used — no padding changes — to avoid layout flicker.
        val decorView = context.activity.window.decorView

        val updateToolbarPosition = {
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)

            val location = IntArray(2)
            native.getLocationOnScreen(location)

            val viewBottom = location[1] + native.height
            val overlap = viewBottom - r.bottom

            if (overlap > 0) {
                toolbarView.translationY = -overlap.toFloat()
            } else {
                toolbarView.translationY = 0f
            }
        }

        // 4. Attach the listeners to fire when the keyboard opens or the user scrolls
        decorView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() = updateToolbarPosition()
        })
        decorView.viewTreeObserver.addOnScrollChangedListener {
            updateToolbarPosition()
        }


        // Listeners for Cursor and Content updates remain unchanged below
        nativeEditText.setOnClickListener {
            val spannable = nativeEditText.text
            val start = nativeEditText.selectionStart
            if (spannable != null && start >= 0) {
                val urlSpans = spannable.getSpans(start, start, URLSpan::class.java)
                if (urlSpans.isNotEmpty()) {
                    editLink(urlSpans.first())
                    return@setOnClickListener
                }
            }
            updateSelectedTags()
        }

        nativeEditText.addTextChangedListener(object : TextWatcher {
            var newlineInsertedPos = -1

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (this@MarkdownRichTextEditor.recursive) return
                // Check if a newline was part of the change (handles autocorrect + enter)
                val changedText = s?.substring(start, start + count) ?: ""
                val newlineIndex = changedText.indexOf('\n')
                if (newlineIndex != -1) {
                    newlineInsertedPos = start + newlineIndex
                } else {
                    newlineInsertedPos = -1
                }
                updateSelectedTags()
            }
            override fun afterTextChanged(s: Editable?) {
                // ... (Keep the rest of your existing text watcher logic exactly as is)
                if (this@MarkdownRichTextEditor.recursive || s == null) return
                val pos = newlineInsertedPos
                if (pos != -1) {
                    newlineInsertedPos = -1
                    val currentLineStart = if (pos > 0) {
                        val lastNewline = s.lastIndexOf('\n', pos - 1)
                        if (lastNewline == -1) 0 else lastNewline + 1
                    } else 0

                    val lineText = s.substring(currentLineStart, pos)
                    val indentMatch = Regex("^(\\s*)").find(lineText)
                    val indent = indentMatch?.groupValues?.get(1) ?: ""
                    val trimmedLine = lineText.trimStart()

                    if (trimmedLine.startsWith("* ")) {
                        // Unordered list
                        if (trimmedLine == "* ") {
                            this@MarkdownRichTextEditor.recursive = true
                            s.delete(currentLineStart, pos + 1)
                            this@MarkdownRichTextEditor.recursive = false
                        } else {
                            this@MarkdownRichTextEditor.recursive = true
                            s.insert(pos + 1, indent + "* ")
                            this@MarkdownRichTextEditor.recursive = false
                            nativeEditText.setSelection(pos + 1 + indent.length + 2)
                        }
                    } else if (trimmedLine.matches(Regex("^\\d+\\.\\s+.*"))) {
                        // Ordered list
                        val match = Regex("^(\\d+)\\.\\s+").find(trimmedLine)
                        if (match != null) {
                            val num = match.groupValues[1].toInt()
                            val prefixLen = match.groupValues[0].length
                            if (trimmedLine.length <= prefixLen) {
                                this@MarkdownRichTextEditor.recursive = true
                                s.delete(currentLineStart, pos + 1)
                                this@MarkdownRichTextEditor.recursive = false
                            } else {
                                this@MarkdownRichTextEditor.recursive = true
                                val nextPrefix = "${num + 1}. "
                                s.insert(pos + 1, indent + nextPrefix)
                                this@MarkdownRichTextEditor.recursive = false
                                nativeEditText.setSelection(pos + 1 + indent.length + nextPrefix.length)
                            }
                        }
                    } else {
                        // Modifier reset
                        val activeSpans = s.getSpans(pos, pos, Any::class.java)
                        activeSpans.forEach { span ->
                            if (span is StyleSpan || span is StrikethroughSpan || span is TypefaceSpan || span is RelativeSizeSpan) {
                                val startSpan = s.getSpanStart(span)
                                if (startSpan != -1 && startSpan < pos) {
                                    s.setSpan(span, startSpan, pos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                            }
                        }

                        // Explicitly remove any spans starting at pos + 1
                        val nextSpans = s.getSpans(pos + 1, pos + 1, Any::class.java)
                        nextSpans.forEach { span ->
                            if (span is StyleSpan || span is StrikethroughSpan || span is TypefaceSpan || span is RelativeSizeSpan) {
                                if (s.getSpanStart(span) == pos + 1) {
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

                    this@MarkdownRichTextEditor.recursive = true
                    nativeEditText.setText(parseMarkdownToSpannable(value))
                    this@MarkdownRichTextEditor.recursive = false

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

    private fun insertLink() {
        val spannable = nativeEditText.text ?: return
        val start = nativeEditText.selectionStart
        val end = nativeEditText.selectionEnd
        if (start < 0) return

        val selectedText = if (start != end && start >= 0 && end >= 0 && end <= spannable.length) {
            spannable.substring(start, end)
        } else ""

        val existingUrlSpans = spannable.getSpans(start, start, URLSpan::class.java)
        if (existingUrlSpans.isNotEmpty()) {
            editLink(existingUrlSpans.first())
            return
        }

        val editor = this
        context.coordinatorFrame?.bottomSheet(
            partialRatio = 0.5f,
            blockBehind = true,
            startState = BottomSheetState.PARTIALLY_EXPANDED
        ) { control ->
            themed(DialogSemantic).col {
                applySafeInsets()
                centered.coordinatorDragHandle()
                text("Insert Link")
                space()
                val urlInput = fieldTheme.textInput { hint = "URL" }
                val textInputWidget = fieldTheme.textInput {
                    hint = "Link text"
                    if (selectedText.isNotEmpty()) content.value = selectedText
                }
                space()
                row {
                    expanding.button {
                        text("Cancel")
                        onClick { control.close() }
                    }
                    important.button {
                        text("Insert")
                        onClick {
                            val url = urlInput.content.value.trim()
                            val text = textInputWidget.content.value.trim()
                            if (url.isNotEmpty()) {
                                control.close()
                                val sp = editor.nativeEditText.text
                                if (sp != null) {
                                    if (start == end) {
                                        val insertText = text.ifEmpty { url }
                                        sp.insert(start, insertText)
                                        sp.setSpan(
                                            URLSpan(url),
                                            start,
                                            start + insertText.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                    } else {
                                        val s = start
                                        val e = end
                                        if (text.isNotEmpty() && text != sp.substring(s, e)) {
                                            sp.replace(s, e, text)
                                            sp.setSpan(
                                                URLSpan(url),
                                                s,
                                                s + text.length,
                                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                            )
                                        } else {
                                            sp.setSpan(URLSpan(url), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                        }
                                    }
                                    editor.updateSelectedTags()
                                    (editor.content as? ContentValue)?.update()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun editLink(span: URLSpan) {
        val spannable = nativeEditText.text ?: return
        val existingS = spannable.getSpanStart(span)
        val existingE = spannable.getSpanEnd(span)
        val existingText =
            if (existingS >= 0 && existingE > existingS) spannable.substring(existingS, existingE) else ""
        val existingUrl = span.url
        val editor = this

        context.coordinatorFrame?.bottomSheet(
            partialRatio = 0.5f,
            blockBehind = true,
            startState = BottomSheetState.PARTIALLY_EXPANDED
        ) { control ->
            themed(DialogSemantic).col {
                applySafeInsets()
                centered.coordinatorDragHandle()
                text("Edit Link")
                space()
                val urlInput = fieldTheme.textInput {
                    hint = "URL"
                    content.value = existingUrl
                }
                val textInputWidget = fieldTheme.textInput {
                    hint = "Link text"
                    content.value = existingText
                }
                space()
                row {
                    expanding.button {
                        text("Cancel")
                        onClick { control.close() }
                    }
                    important.button {
                        text("Remove")
                        onClick {
                            control.close()
                            val sp = editor.nativeEditText.text
                            if (sp != null) {
                                val ss = sp.getSpanStart(span)
                                val ee = sp.getSpanEnd(span)
                                sp.removeSpan(span)
                                if (ss >= 0 && ee > ss) {
                                    val linkText = sp.substring(ss, ee)
                                    sp.replace(ss, ee, linkText)
                                }
                                editor.updateSelectedTags()
                                (editor.content as? ContentValue)?.update()
                            }
                        }
                    }
                    expanding.space()
                    important.button {
                        text("Update")
                        onClick {
                            val url = urlInput.content.value.trim()
                            val text = textInputWidget.content.value.trim()
                            if (url.isNotEmpty()) {
                                control.close()
                                val sp = editor.nativeEditText.text
                                if (sp != null) {
                                    val ss = sp.getSpanStart(span)
                                    val ee = sp.getSpanEnd(span)
                                    sp.removeSpan(span)
                                    if (ss >= 0 && ee > ss && text.isNotEmpty()) {
                                        sp.replace(ss, ee, text)
                                        sp.setSpan(
                                            URLSpan(url),
                                            ss,
                                            ss + text.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                    } else if (ss >= 0) {
                                        sp.setSpan(URLSpan(url), ss, ee, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                    editor.updateSelectedTags()
                                    (editor.content as? ContentValue)?.update()
                                }
                            }
                        }
                    }
                }
            }
        }
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

                is URLSpan -> tags.add(RichTextTags.LINK)
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

        data class LinkInfo(val start: Int, val end: Int, val url: String)

        val linkSpans = spannable.getSpans(0, length, URLSpan::class.java)
            .mapNotNull { span ->
                val s = spannable.getSpanStart(span)
                val e = spannable.getSpanEnd(span)
                if (s >= 0 && e >= 0 && e > s) LinkInfo(s, e, span.url) else null
            }.sortedBy { it.start }

        var currentStyles = emptyList<String>()
        val styleOrder = listOf("~~", "**", "*", "`")
        var linkIdx = 0
        var i = 0

        while (i < length) {
            val currentLink = linkSpans.getOrNull(linkIdx)?.takeIf { it.start == i }
            if (currentLink != null) {
                currentStyles.reversed().forEach { result.append(it) }
                currentStyles = emptyList()
                result.append("[")
                for (j in currentLink.start until currentLink.end) {
                    result.append(spannable[j])
                }
                result.append("](${currentLink.url})")
                i = currentLink.end
                linkIdx++
                continue
            }

            if (linkSpans.getOrNull(linkIdx)?.let { i in it.start until it.end } == true) {
                i++
                continue
            }

            val char = spannable[i]

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

            val activeButShouldBeClosed = currentStyles.filter { it !in stylesAtI }
            if (activeButShouldBeClosed.isNotEmpty()) {
                currentStyles.reversed().forEach { result.append(it) }
                currentStyles = emptyList()
            }

            styleOrder.forEach { style ->
                if (style in stylesAtI && style !in currentStyles) {
                    result.append(style)
                    currentStyles = currentStyles + style
                }
            }

            result.append(char)
            i++
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
        return builder
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

            is MarkdownNode.Link -> {
                val startPos = builder.length
                node.children.forEach { renderNodeToSpannable(it, builder) }
                builder.setSpan(URLSpan(node.url), startPos, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            else -> {}
        }
    }
}