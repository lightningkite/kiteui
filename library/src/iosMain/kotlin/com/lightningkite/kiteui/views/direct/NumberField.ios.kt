package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName
import com.lightningkite.kiteui.WeakReference
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue


actual class NumberInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = WrapperView()
    val toolbar = UIToolbar().apply {
        barStyle = UIBarStyleDefault
        setTranslucent(true)
        sizeToFit()
        setItems(listOf(
            UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
            UIBarButtonItem(title = "Done", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain, target = this@NumberInput, action =sel_registerName("done")),
        ), animated = false)
    }
    val textField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        keyboardType = UIKeyboardTypeDecimalPad
        delegate = NextFocusDelegateShared
        inputAccessoryView = toolbar
    }

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

    override fun applyForeground(theme: Theme) {
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }

    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    fun updateHint() {
        textField.placeholder = hint
        // TODO: Colored hint
//        textField.attributedPlaceholder = hint
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    actual val content: ImmediateWritable<Double?> = object : ImmediateWritable<Double?> {
        override var value: Double?
            get() = (textField.text ?: "").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            set(value) {
                if(textField.text != (value?.commaString() ?: ""))
                    textField.text = value?.commaString() ?: ""
            }
        override fun addListener(listener: () -> Unit): () -> Unit {
            return textField.onEvent(this@NumberInput, UIControlEventEditingChanged, listener)
        }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            textField.autocapitalizationType = when (value.case) {
                KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
                KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
                KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
                KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
            }
            textField.keyboardType = when (value.type) {
                KeyboardType.Text -> UIKeyboardTypeDefault
                KeyboardType.Integer -> UIKeyboardTypeNumberPad
                KeyboardType.Phone -> UIKeyboardTypePhonePad
                KeyboardType.Decimal -> UIKeyboardTypeNumbersAndPunctuation
                KeyboardType.Email -> UIKeyboardTypeEmailAddress
            }
            textField.textContentType = when (value.autocomplete) {
                AutoComplete.Email -> UITextContentTypeUsername
                AutoComplete.Password -> UITextContentTypePassword
                AutoComplete.NewPassword -> UITextContentTypeNewPassword
                else -> null
            }
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }

    @ObjCAction
    fun done() {
        action?.startAction(this@NumberInput)
        CoroutineScope(Dispatchers.Main.immediate).launch {
            delay(16)
            val fistResponderChild = this@NumberInput.textField.window?.findFirstResponderChild()
            if(fistResponderChild == this@NumberInput.textField){
                this@NumberInput.textField.resignFirstResponder()
                return@launch
            }
            if (fistResponderChild is UITextField) {
                val keyboardType = fistResponderChild.keyboardType
                if (keyboardType != UIKeyboardTypeDecimalPad) {
                    this@NumberInput.textField.resignFirstResponder()
                }
            }
        }
    }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
            textField.delegate = value?.let {
                val d = object : NSObject(), UITextFieldDelegateProtocol {
                    override fun textFieldShouldReturn(textField: UITextField): Boolean {
                        it?.startAction(this@NumberInput)
                        return true
                    }
                }
                textField.extensionStrongRef = d
                d
            } ?: NextFocusDelegateShared
            textField.returnKeyType = when (value?.title) {
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
    actual var hint: String = ""
        set(value) {
            field = value
            updateHint()
        }
    actual inline var align: Align
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
    actual var range: ClosedRange<Double>? = null

    actual var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!textField.enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
