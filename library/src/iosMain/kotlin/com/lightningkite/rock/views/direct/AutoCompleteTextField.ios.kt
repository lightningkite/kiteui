package com.lightningkite.rock.views.direct

import com.lightningkite.rock.models.Action
import com.lightningkite.rock.models.KeyboardCase
import com.lightningkite.rock.models.KeyboardHints
import com.lightningkite.rock.models.KeyboardType
import com.lightningkite.rock.reactive.Writable
import com.lightningkite.rock.views.*
import platform.UIKit.*
import platform.darwin.NSObject

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NAutoCompleteTextField = UITextField

@ViewDsl
actual fun ViewWriter.autoCompleteTextFieldActual(setup: AutoCompleteTextField.() -> Unit): Unit = stack {
    element(UITextField()) {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        handleTheme(this) { textColor = it.foreground.closestColor().toUiColor() }
        calculationContext.onRemove {
            extensionStrongRef = null
        }
        setup(AutoCompleteTextField(this))
    }
}

actual val AutoCompleteTextField.content: Writable<String>
    get() = object : Writable<String> {
        override suspend fun awaitRaw(): String = native.text ?: ""
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventEditingChanged) {
                listener()
            }
        }

        override suspend fun set(value: String) {
            native.text = value
        }
    }
actual inline var AutoCompleteTextField.keyboardHints: KeyboardHints
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
actual var AutoCompleteTextField.action: Action?
    get() = TODO()
    set(value) {
        native.delegate = action?.let {
            val d = object : NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    launch { it.onSelect() }
                    return true
                }
            }
            native.extensionStrongRef = d
            d
        } ?: NextFocusDelegateShared
        native.returnKeyType = when (action?.title) {
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

//actual inline var AutoCompleteTextField.hint: String
//    get() = native.placeholder ?: ""
//    set(value) {
//        native.placeholder = value
//    }
actual inline var AutoCompleteTextField.suggestions: List<String>
    get() = TODO()
    set(value) {

    }