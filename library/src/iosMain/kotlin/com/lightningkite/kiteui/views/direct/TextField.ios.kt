package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.KeyboardCase
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.KeyboardType
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*
import platform.UIKit.*
import platform.darwin.NSObject

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextField = UITextField

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit): Unit = stack {
    element(UITextField()) {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        handleTheme(this, viewLoads = true, foreground = { textColor = it.foreground.closestColor().toUiColor() },) {
            calculationContext.onRemove {
                extensionStrongRef = null
            }
            setup(TextField(this))
        }
    }
}

actual val TextField.content: Writable<String>
    get() = object : Writable<String> {
        override val state get() = ReadableState(native.text ?: "")
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventEditingChanged) {
                listener()
            }
        }

        override suspend fun set(value: String) {
            native.text = value
        }
    }
actual inline var TextField.keyboardHints: KeyboardHints
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
actual var TextField.action: Action?
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
actual inline var TextField.hint: String
    get() = native.placeholder ?: ""
    set(value) {
        native.placeholder = value
    }
actual inline var TextField.range: ClosedRange<Double>?
    get() = TODO()
    set(value) {}