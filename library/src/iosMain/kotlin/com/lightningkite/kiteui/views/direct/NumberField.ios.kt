package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NNumberField = UITextField

@ViewDsl
actual inline fun ViewWriter.numberFieldActual(crossinline setup: NumberField.() -> Unit): Unit = stack {
    element(UITextField()) {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
//        delegate = AutoFormatter
        var block = false
        onEvent(UIControlEventEditingChanged) {
            if (block) return@onEvent
            block = true
            try {
                numberAutocommaRepair(
                    dirty = text ?: "",
                    selectionStart = selectedTextRange?.start?.let { offsetFromPosition(beginningOfDocument, it) }?.toInt(),
                    selectionEnd = selectedTextRange?.end?.let { offsetFromPosition(beginningOfDocument, it) }?.toInt(),
                    setResult = {
                        text = it
                    },
                    setSelectionRange = { start, end ->
                        selectedTextRange = textRangeFromPosition(
                            positionFromPosition(beginningOfDocument, start.toLong()) ?: return@numberAutocommaRepair,
                            positionFromPosition(beginningOfDocument, end.toLong()) ?: return@numberAutocommaRepair,
                        )
                    }
                )
            } finally {
                block = false
            }
        }
        handleTheme(this, viewLoads = true, foreground = {
            textColor = it.foreground.closestColor().toUiColor()
//            attributedPlaceholder = NSMutableAttributedString()
        }) {
            calculationContext.onRemove {
                extensionStrongRef = null
            }
            setup(NumberField(this).also { it.keyboardHints = KeyboardHints.decimal; it.align = Align.End })
        }
    }
}

//object AutoFormatter: NSObject(), UITextFieldDelegateProtocol {
//    override fun textField
//}

actual val NumberField.content: Writable<Double?>
    get() = object : Writable<Double?> {
        override val state get() = ReadableState((native.text ?: "").filter { it.isDigit() || it == '.' }.toDoubleOrNull())
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventEditingChanged) {
                listener()
            }
        }

        override suspend fun set(value: Double?) {
            native.text = value?.commaString() ?: ""
        }
    }
actual inline var NumberField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.autocapitalizationType = when (value.case) {
            KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
            KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
            KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
        }
        native.keyboardType = when (value.type) {
            KeyboardType.Text -> UIKeyboardTypeDefault
            KeyboardType.Integer -> UIKeyboardTypeNumberPad
            KeyboardType.Phone -> UIKeyboardTypePhonePad
            KeyboardType.Decimal -> UIKeyboardTypeNumbersAndPunctuation
            KeyboardType.Email -> UIKeyboardTypeEmailAddress
        }
    }
actual var NumberField.action: Action?
    get() = TODO()
    set(value) {
        native.delegate = value?.let {
            val d = object : NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    launch { it.onSelect() }
                    return true
                }
            }
            native.extensionStrongRef = d
            d
        } ?: NextFocusDelegateShared
        native.returnKeyType = when (value?.title) {
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
actual inline var NumberField.hint: String
    get() = native.placeholder ?: ""
    set(value) {
        native.placeholder = value
    }
actual inline var NumberField.range: ClosedRange<Double>?
    get() = TODO()
    set(value) {}
actual inline var NumberField.align: Align
    get() = when (native.textAlignment) {
        NSTextAlignmentLeft -> Align.Start
        NSTextAlignmentCenter -> Align.Center
        NSTextAlignmentRight -> Align.End
        NSTextAlignmentJustified -> Align.Stretch
        else -> Align.Start
    }
    set(value) {
        native.contentMode = when (value) {
            Align.Start -> UIViewContentMode.UIViewContentModeLeft
            Align.Center -> UIViewContentMode.UIViewContentModeCenter
            Align.End -> UIViewContentMode.UIViewContentModeRight
            Align.Stretch -> UIViewContentMode.UIViewContentModeScaleAspectFit
        }
        native.textAlignment = when (value) {
            Align.Start -> NSTextAlignmentLeft
            Align.Center -> NSTextAlignmentCenter
            Align.End -> NSTextAlignmentRight
            Align.Stretch -> NSTextAlignmentJustified
        }
    }
actual inline var NumberField.textSize: Dimension
    get() = native.font?.pointSize?.let(::Dimension) ?: 1.rem
    set(value) {
        native.extensionFontAndStyle?.let {
            native.font = it.font.get(value.value, it.weight.toUIFontWeight(), it.italic)
        }
    }