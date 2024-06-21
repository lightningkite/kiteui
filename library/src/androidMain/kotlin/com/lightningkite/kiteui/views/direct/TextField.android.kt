package com.lightningkite.kiteui.views.direct

import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.models.*
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
        }.let {
            autofillHints?.let { hints ->
                return if (hints.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS)) it.copy(autocomplete = AutoComplete.Email)
                else if (hints.contains(View.AUTOFILL_HINT_PASSWORD)) it.copy(autocomplete = AutoComplete.Password)
                else if (hints.contains(View.AUTOFILL_HINT_PHONE)) it.copy(autocomplete = AutoComplete.Phone)
                else it
            } ?: it
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
        }.let {
            if (value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword))
                it or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else it
        }
        n.inputType = inputType
        when (value.autocomplete) {
            AutoComplete.Email -> View.AUTOFILL_HINT_EMAIL_ADDRESS
            AutoComplete.Password, AutoComplete.NewPassword -> View.AUTOFILL_HINT_PASSWORD
            AutoComplete.Phone -> View.AUTOFILL_HINT_PHONE
            null -> null
        }?.let { n.setAutofillHints(it) }
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
actual var TextField.align: Align
    get() {
        return when (native.gravity) {
            Gravity.START -> Align.Start
            Gravity.END -> Align.End
            Gravity.CENTER -> Align.Center
            Gravity.CENTER_VERTICAL -> Align.Start
            Gravity.CENTER_HORIZONTAL -> Align.Center
            else -> Align.Start
        }
    }
    set(value) {
        when (value) {
            Align.Start -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
            Align.End -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_END
            Align.Center -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_CENTER
            Align.Stretch -> {
                native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
                native.updateLayoutParams<ViewGroup.LayoutParams> {
                    this.width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }
    }
actual var TextField.textSize: Dimension
    get() {
        return Dimension(native.textSize)
    }
    set(value) {
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
    }

actual inline var TextField.enabled: Boolean
    get() = native.isEnabled
    set(value) {
        native.isEnabled = value
    }

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit) {
    return viewElement(factory = ::EditText, wrapper = ::TextField) {
        handleTheme<TextView>(native, foreground = applyTextColorFromTheme, viewLoads = true) {
            setup(this)
        }
    }
}