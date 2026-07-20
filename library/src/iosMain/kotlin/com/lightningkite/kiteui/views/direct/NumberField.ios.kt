package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName


public actual class NumberInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = numberInputDriverValue()
    override val driverActions get() = super.driverActions + numberInputDriverActions()
    override val native = WrapperView()
    public val trigger: NSObject = object : NSObject() {
        @ObjCAction
        fun done() {
            action?.let {
                textField.resignFirstResponder()
                it.startAction(this@NumberInput)
            } ?: NextFocusDelegateShared.textFieldShouldReturn(textField)
        }
    }
    public val textField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        keyboardType = UIKeyboardTypeDecimalPad
        delegate = NextFocusDelegateShared

        // Explicit frame prevents UnsatisfiableConstraints error when automatic constraints are set by the system
        // https://stackoverflow.com/questions/54284029/uitoolbar-with-uibarbuttonitem-layoutconstraint-issue
        inputAccessoryView = UIToolbar(CGRectMake(0.0, 0.0, UIScreen.mainScreen.bounds.useContents { size.width }, 35.0)).apply {
            barStyle = UIBarStyleDefault
            setTranslucent(true)
            sizeToFit()
            setItems(
                listOf(
                    UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                    UIBarButtonItem(title = "Done", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain, target = trigger, action = sel_registerName("done")),
                ), animated = false
            )
        }
    }
    override val control: UIControl get() = textField

    init {
        native.addSubview(textField)
    }

    init {
        var block = false
        textField.onEvent(this@NumberInput, UIControlEventEditingChanged) {
            if (block) return@onEvent
            block = true
            try {
                numberAutocommaRepair(
                    dirty = textField.text ?: "",
                    selectionStart = textField.selectedTextRange?.start?.let { textField.offsetFromPosition(textField.beginningOfDocument, it) }?.toInt(),
                    selectionEnd = textField.selectedTextRange?.end?.let { textField.offsetFromPosition(textField.beginningOfDocument, it) }?.toInt(),
                    allowDecimal = keyboardHints.type.allowDecimal,
                    setResult = {
                        textField.text = it
                    },
                    setSelectionRange = { start, end ->
                        textField.selectedTextRange = textField.textRangeFromPosition(
                            textField.positionFromPosition(textField.beginningOfDocument, start.toLong()) ?: return@numberAutocommaRepair,
                            textField.positionFromPosition(textField.beginningOfDocument, end.toLong()) ?: return@numberAutocommaRepair,
                        )
                    }
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
        applyAlign(_align ?: theme.theme.font.align)
    }

    public fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    public fun updateHint() {
        textField.placeholder = hint
        // TODO: Colored hint
//        textField.attributedPlaceholder = hint
    }

    public var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    public actual val content: MutableReactiveValue<Double?> = object : MutableReactiveValue<Double?> {
        override var value: Double?
            get() = (textField.text ?: "").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            set(value) {
                if (textField.text != (value?.commaString() ?: "")) {
                    textField.text = value?.commaString() ?: ""
                    // fire change event so reactive listeners are notified on programmatic updates
                    textField.sendActionsForControlEvents(UIControlEventEditingChanged)
                }
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            return textField.onEvent(this@NumberInput, UIControlEventEditingChanged, listener)
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
            textField.accessibilityHint = value.ifEmpty { null }
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

    public actual var range: ClosedRange<Double>? = null
}
