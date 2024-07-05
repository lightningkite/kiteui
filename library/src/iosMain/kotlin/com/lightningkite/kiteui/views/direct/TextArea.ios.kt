package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardCase
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.KeyboardType
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.views.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.listen

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextArea = UITextView

@ViewDsl
actual inline fun ViewWriter.textAreaActual(crossinline setup: TextArea.() -> Unit): Unit = stack {
    element(UITextView()) {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        val dg = TextAreaDelegate()
        delegate = dg
        extensionStrongRef = dg
        handleTheme(this, viewLoads = true, foreground = { textColor = it.foreground.closestColor().toUiColor() },) {
            setup(TextArea(this))
            calculationContext.onRemove {
                extensionStrongRef = null
            }
        }
    }
}

class TextAreaDelegate(): NSObject(), UITextViewDelegateProtocol {
    val listeners = ArrayList<()->Unit>()
    override fun textViewDidChange(textView: UITextView) {
        listeners.invokeAllSafe()
    }
}

actual val TextArea.content: Writable<String>
    get() = object : Writable<String> {
        override val state get() = ReadableState(native.text)
        override fun addListener(listener: () -> Unit): () -> Unit {
            val dg = native.extensionStrongRef as TextAreaDelegate
            dg.listeners.add(listener)
            return {
                val i = dg.listeners.indexOf(listener)
                if(i != -1) dg.listeners.removeAt(i)
            }
        }

        override suspend fun set(value: String) {
            native.text = value
        }
    }
actual inline var TextArea.keyboardHints: KeyboardHints
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
actual inline var TextArea.hint: String
    get() = TODO()
    set(value) {}

actual inline var TextArea.enabled: Boolean
    get() = native.editable
    set(value) {
        native.editable = value
    }