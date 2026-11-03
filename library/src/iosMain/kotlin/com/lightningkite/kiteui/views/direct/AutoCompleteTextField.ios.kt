package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.MutableReactiveValue
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import com.lightningkite.kiteui.views.AiDriver


public actual class AutoCompleteTextField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + autoCompleteDriverActions()
    override val native: WrapperView = WrapperView()
    public val textField: UITextField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
    }
    override val control: UIControl get() = textField

    init {
        native.addSubview(textField)
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        textField.textColor = theme.theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.theme.font
        // The placeholder colour is baked into the attributed string at draw time, so it has to be
        // reapplied whenever the theme changes - see updateHint().
        updateHint()
    }

    internal fun updateFont() {
        val textSize = textSize
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(textSize.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(textSize.value)
        textField.textAlignment = alignment
    }

    internal fun updateHint() {
        // A plain `placeholder` draws in a fixed system grey that is all but invisible on a dark
        // theme, so the colour is set explicitly from the current theme instead - see TextField.ios.kt's
        // updateHint() for the same fix on the plain text field.
        textField.attributedPlaceholder = NSAttributedString.create(
            hint,
            mapOf(NSForegroundColorAttributeName to theme.foreground.closestColor().withAlpha(0.5f).toUiColor())
        )
    }

    public var textSize: Dimension = 1.rem
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    public var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    public actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String> {
        override var value: String
            get() = textField.text ?: ""
            set(value) {
                if (textField.text == value) return
                textField.text = value
                // fire change event so reactive listeners are notified on programmatic updates
                textField.sendActionsForControlEvents(UIControlEventEditingChanged)
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            return textField.onEvent(this@AutoCompleteTextField, UIControlEventEditingChanged, listener)
        }
    }

    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            textField.autocapitalizationType = value.case.ios
            textField.keyboardType = value.type.ios
            textField.textContentType = value.autocomplete.iosTextContentType
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }

    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    override fun nativeSetAction(action: Action?) {
        textField.delegate = action?.let { action ->
            // Use weak reference to avoid retain cycle
            val weakSelf = kotlin.native.ref.WeakReference(this)
            val d = object : NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    weakSelf.get()?.let { action.startAction(it) }
                    return true
                }
            }
            textField.extensionStrongRef = d
            d
        } ?: NextFocusDelegateShared
        textField.returnKeyType = when (action?.title) {
            "Emergency Call" -> UIReturnKeyType.UIReturnKeyEmergencyCall
            "Go" -> UIReturnKeyType.UIReturnKeyGo
            "Next" -> UIReturnKeyType.UIReturnKeyNext
            "Continue" -> UIReturnKeyType.UIReturnKeyContinue
            "Default" -> UIReturnKeyType.UIReturnKeyDefault
            "Join" -> UIReturnKeyType.UIReturnKeyJoin
            "Done" -> UIReturnKeyType.UIReturnKeyDone
            "Yahoo" -> UIReturnKeyType.UIReturnKeyYahoo
            "Send" -> UIReturnKeyType.UIReturnKeySend
            "Google" -> UIReturnKeyType.UIReturnKeyGoogle
            "Route" -> UIReturnKeyType.UIReturnKeyRoute
            "Search" -> UIReturnKeyType.UIReturnKeySearch
            else -> UIReturnKeyType.UIReturnKeyDone
        }
    }

    public actual var hint: String = ""
        set(value) {
            field = value
            updateHint()
        }

    public inline var align: Align
        get() = when (textField.textAlignment) {
            NSTextAlignmentLeft -> Align.Start
            NSTextAlignmentCenter -> Align.Center
            NSTextAlignmentRight -> Align.End
            NSTextAlignmentJustified -> Align.Stretch
            else -> Align.Start
        }
        set(value) {
            textField.contentMode = when (value) {
                Align.Start -> UIViewContentMode.UIViewContentModeLeft
                Align.Center -> UIViewContentMode.UIViewContentModeCenter
                Align.End -> UIViewContentMode.UIViewContentModeRight
                Align.Stretch -> UIViewContentMode.UIViewContentModeScaleAspectFit
            }
            textField.textAlignment = when (value) {
                Align.Start -> NSTextAlignmentLeft
                Align.Center -> NSTextAlignmentCenter
                Align.End -> NSTextAlignmentRight
                Align.Stretch -> NSTextAlignmentJustified
            }
        }

    public actual var suggestions: List<String> = listOf()
        set(value) {
            field = value
            recomputeSuggestionsPopover()
        }

    // TODO: WTF, this is HUUUUUUUGE

    // Tracks first-responder state via the standard UIControl editing-session events rather than
    // isFirstResponder polling - these already fire at exactly the right moments (focus gained/lost).
    // internal rather than private so a test can drive it directly - see recomputeSuggestionsPopover's
    // doc comment for why the UIControl events themselves can't be synthesized in a test.
    internal var focused: Boolean = false
        set(value) {
            field = value
            recomputeSuggestionsPopover()
        }

    /**
     * Case-insensitive prefix match against the whole suggestion or any individual
     * whitespace-separated word within it - e.g. typing "gra" matches both "Grape" and
     * "Dragon Fruit". This mirrors Android's `AutoCompleteTextView` default `ArrayAdapter` filter
     * (`ArrayFilter.performFiltering`), so the three platforms agree on what counts as a match.
     */
    private fun matches(suggestion: String, query: String): Boolean =
        suggestion.startsWith(query, ignoreCase = true) ||
                // Split on any whitespace, not just ' ', so a tab- or newline-separated label
                // behaves the same as a space-separated one.
                suggestion.split(Regex("\\s+")).any { it.startsWith(query, ignoreCase = true) }

    private var popoverOpen = false

    // The col inside the open popover holding the suggestion rows, so recomputeSuggestionsPopover()
    // can redraw it in place (while staying open) instead of calling openPopover() again per
    // keystroke - that would flicker and risk stacking popovers. Null whenever the popover is closed.
    private var popoverRows: ContainerElement? = null

    /**
     * The suggestion most recently picked, while the field still contains exactly it.
     *
     * A selection writes the chosen text into `content`, and that text matches the suggestion it
     * came from - so a naive recompute reopens the dropdown on the value the user just dismissed it
     * by choosing. This says "the field holds a completed selection" rather than "skip the next
     * recompute", which is what an earlier version did: counting recomputes ties the behaviour to
     * *when* UIKit delivers `UIControlEventEditingChanged`, and that differs between a real app and
     * a test binary, so the guard silently protected the wrong call in one of them. Comparing
     * content instead gives the same answer however many times recompute runs, or none.
     */
    private var completedSelection: String? = null

    // internal rather than private: a real tap on a suggestion row can't be driven from a test
    // without simulating actual UIKit touch delivery, which - like UIGestureRecognizer.setState()
    // (see HintPopoverTest's doc comment) - doesn't work outside UIKit's own touch pipeline in this
    // environment. Exposing the row-tap handler directly lets tests call it as an ordinary Kotlin
    // function instead.
    internal fun selectSuggestion(value: String) {
        completedSelection = value
        content.value = value
        closePopoverIfOpen()
    }

    private fun closePopoverIfOpen() {
        val rows = popoverRows ?: return
        popoverOpen = false
        popoverRows = null
        // Closed through the popover's *own* context, not this field's.
        //
        // closePopovers() dismisses the popover the context is inside, walking up via
        // popoverParent. This field is the popover's anchor, not its content, so its context has
        // neither a closer nor a popover parent - calling it here did nothing at all, which is why
        // picking a suggestion left the list on screen and why a query that stopped matching left
        // an empty box behind. `rows` is the container created inside openPopover, so its context
        // is inside the popover and resolves correctly.
        rows.context.closePopovers()
    }

    /**
     * Opens/closes the suggestions dropdown and (re)draws its rows. Called from the real
     * `UIControlEventEditingChanged`/`DidBegin`/`DidEnd` listeners below in normal use.
     *
     * internal rather than private: `sendActionsForControlEvents` - the standard UIKit mechanism
     * `content`'s own setter uses to notify listeners of a programmatic change, and the only way to
     * simulate typing without a real touch/keyboard - does not deliver target-action callbacks in a
     * bare test binary (confirmed empirically: neither `content`'s own listener nor a directly
     * `addTarget`-registered one fire there, though both work in the real app, which runs a genuine
     * `UIApplicationMain` event loop). So instead of relying on those events to trigger recomputation
     * reactively, this is called directly - both by the real listeners and, in tests, after manually
     * writing `content.value`/`focused`.
     */
    internal fun recomputeSuggestionsPopover() {
        val query = content.value
        // The field still holds exactly what was just picked, so there is nothing left to suggest.
        // Any edit changes `query` and clears this, so typing on resumes normally.
        if (query == completedSelection) {
            closePopoverIfOpen()
            return
        }
        completedSelection = null
        val matchesNow = if (query.isBlank()) listOf() else suggestions.filter { matches(it, query) }
        val shouldBeOpen = focused && matchesNow.isNotEmpty()
        if (shouldBeOpen) {
            val rows = popoverRows
            if (rows == null) {
                popoverOpen = true
                // openPopover only adds sibling views to the overlay frame - it never touches
                // first-responder state, so the keyboard stays up and this field keeps typing focus
                // while the dropdown is visible (verified in AutoCompleteSuggestionsIosTest).
                openPopover(PopoverPreferredDirection.belowLeft) {
                    col {
                        this@AutoCompleteTextField.popoverRows = this
                        this@AutoCompleteTextField.drawRows(this, matchesNow)
                    }
                }
            } else {
                drawRows(rows, matchesNow)
            }
        } else {
            closePopoverIfOpen()
        }
    }

    private fun drawRows(container: ContainerElement, items: List<String>) {
        container.clearChildren()
        for (suggestion in items) {
            container.button {
                text(suggestion)
                onClick { this@AutoCompleteTextField.selectSuggestion(suggestion) }
            }
        }
    }

    init {
        textField.onEvent(this, UIControlEventEditingDidBegin) { focused = true }
        textField.onEvent(this, UIControlEventEditingDidEnd) { focused = false }
        textField.onEvent(this, UIControlEventEditingChanged) { recomputeSuggestionsPopover() }
        onRemove { closePopoverIfOpen() }
    }
}

