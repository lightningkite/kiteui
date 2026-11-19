package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.reformatWithSelection
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import com.lightningkite.kiteui.views.AiDriver

public actual class FormattedTextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = formattedTextInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + formattedTextInputDriverActions()
    override val native: WrapperView = WrapperView()
    internal val textField: UITextField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        delegate = NextFocusDelegateShared
    }
    override val control: UIControl get() = textField

    init {
        native.addSubview(textField)
    }

    init {
        var block = false
        textField.onEvent(this@FormattedTextInput, UIControlEventEditingChanged) {
            if (block) return@onEvent
            block = true
            try {
                val typed = textField.text ?: ""
                val range = textField.selectedTextRange
                val selection = if (range == null) typed.length until typed.length
                else textField.offsetFromPosition(textField.beginningOfDocument, range.start).toInt() until
                    textField.offsetFromPosition(textField.beginningOfDocument, range.end).toInt()
                val result = format.reformatWithSelection(typed, selection)
                if (result.text != typed) textField.text = result.text
                textField.selectedTextRange = textField.textRangeFromPosition(
                    textField.positionFromPosition(textField.beginningOfDocument, result.selection.first.toLong()) ?: return@onEvent,
                    textField.positionFromPosition(textField.beginningOfDocument, result.selection.last + 1L) ?: return@onEvent,
                )
            } finally {
                block = false
            }
        }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        textField.textColor = theme.theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.theme.font
        updateHint()
        applyAlign(_align ?: theme.theme.font.align)
    }

    internal fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    internal fun updateHint() {
        textField.attributedPlaceholder = NSAttributedString.create(
            hint,
            mapOf(NSForegroundColorAttributeName to theme.foreground.closestColor().withAlpha(0.5f).toUiColor())
        )
    }

    internal var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    public actual var format: TextFormat = TextFormat.None
        set(value) {
            val data = content.value
            field = value
            content.value = data
        }

    public actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String> {
        override var value: String
            get() = (textField.text ?: "").filter(format::isRawData)
            set(value) {
                val formatted = format.format(value.filter(format::isRawData))
                if (textField.text != formatted) {
                    textField.text = formatted
                    // fire change event so reactive listeners are notified on programmatic updates
                    textField.sendActionsForControlEvents(UIControlEventEditingChanged)
                }
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            return textField.onEvent(this@FormattedTextInput, UIControlEventEditingChanged, listener)
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
    private var _align: Align? = null
    public actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: fontAndStyle?.align ?: Align.Start)
        }

    private fun applyAlign(value: Align) {
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
}