package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*
import platform.Foundation.NSAttributedString
import platform.Foundation.NSAttributedStringMeta
import platform.Foundation.NSMutableAttributedString
import platform.Foundation.NSString
import platform.UIKit.*
import platform.darwin.NSObject

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextField = UITextField

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit): Unit = stack {
    element(UITextField()) {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        handleTheme(this, viewLoads = true, foreground = {
            textColor = it.foreground.closestColor().toUiColor()
//            attributedPlaceholder = NSMutableAttributedString()
        }) {
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
        native.textContentType = when (value.autocomplete) {
            AutoComplete.Email -> UITextContentTypeUsername
            AutoComplete.Password -> UITextContentTypePassword
            AutoComplete.NewPassword -> UITextContentTypeNewPassword
            else -> null
        }
        native.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
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
actual inline var TextField.align: Align
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
actual inline var TextField.textSize: Dimension
    get() = native.font?.pointSize?.let(::Dimension) ?: 1.rem
    set(value) {
        native.extensionFontAndStyle?.let {
            native.font = it.font.get(value.value, if (it.bold) UIFontWeightBold else UIFontWeightRegular, it.italic)
        }
    }