package com.lightningkite.kiteui.views.direct

import android.text.InputType
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.KeyboardCase
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.KeyboardType
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewAction
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.launch

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextField = EditText

actual val TextField.content: Writable<String>
    get() {
        return this@content.native.content
    }
var EditText.keyboardHints: KeyboardHints
    get() {
        return when (inputType) {
            InputType.TYPE_CLASS_NUMBER -> KeyboardHints(KeyboardCase.None, KeyboardType.Integer)
            InputType.TYPE_CLASS_TEXT -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_MASK_CLASS -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_MASK_VARIATION -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_MASK_FLAGS -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_NULL -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS -> KeyboardHints(KeyboardCase.Letters, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_CAP_WORDS -> KeyboardHints(KeyboardCase.Words, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES -> KeyboardHints(KeyboardCase.Sentences, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_AUTO_CORRECT -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_MULTI_LINE -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_FLAG_ENABLE_TEXT_CONVERSION_SUGGESTIONS -> KeyboardHints(
                KeyboardCase.None,
                KeyboardType.Text
            )

            InputType.TYPE_TEXT_VARIATION_URI -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> KeyboardHints(KeyboardCase.None, KeyboardType.Email)
            InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT -> KeyboardHints(KeyboardCase.Words, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> KeyboardHints(KeyboardCase.Words, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> KeyboardHints(KeyboardCase.Words, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_PASSWORD -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_FILTER -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_PHONETIC -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> KeyboardHints(KeyboardCase.None, KeyboardType.Email)
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            InputType.TYPE_CLASS_PHONE -> KeyboardHints(KeyboardCase.None, KeyboardType.Phone)
            InputType.TYPE_CLASS_DATETIME -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
            else -> KeyboardHints(KeyboardCase.None, KeyboardType.Text)
        }
    }
    set(value) {
        val n = this
        val inputType = when (value.type) {
            KeyboardType.Integer -> InputType.TYPE_CLASS_NUMBER
            KeyboardType.Decimal -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            KeyboardType.Text -> {
                when (value.case) {
                    KeyboardCase.Words -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    KeyboardCase.Letters -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    KeyboardCase.Sentences -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    else -> 0
                } or InputType.TYPE_CLASS_TEXT
            }

            KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
            KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        n.inputType = inputType
    }
actual var TextField.keyboardHints: KeyboardHints
    get() {
        return native.keyboardHints
    }
    set(value) {
        native.keyboardHints = value
    }
actual var TextField.action: Action?
    get() {
        return ViewAction[native]
    }
    set(value) {
        ViewAction[native] = value
        native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            launch {
                value?.onSelect?.invoke()
            }
            value != null
        }
    }
actual var TextField.hint: String
    get() {
        return this@hint.native.hint.toString()
    }
    set(value) {
        this@hint.native.hint = value
    }
actual var TextField.range: ClosedRange<Double>?
    get() {
        return native.tag as? ClosedRange<Double>
    }
    set(value) {
        if (value == null) return

        native.tag = value
        native.doAfterTextChanged {
            try {
                if (it == null) return@doAfterTextChanged

                val string = it.toString()
                val doubleValue = string.toDouble()
                if (doubleValue < value.start || doubleValue > value.endInclusive) {
                    val newValue = doubleValue.coerceIn(value)
                    it.clear()
                    it.append(newValue.toString())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit) {
    return viewElement(factory = ::EditText, wrapper = ::TextField) {
        handleTheme<TextView>(native, foreground = applyTextColorFromTheme, viewLoads = true) {
            setup(this)
        }
    }
}