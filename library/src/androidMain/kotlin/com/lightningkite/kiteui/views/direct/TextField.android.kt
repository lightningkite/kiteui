package com.lightningkite.kiteui.views.direct

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.*

actual open class TextInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = EditText(context.activity)
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setTextColor(theme.foreground.colorInt())
        native.setHintTextColor(theme.foreground.closestColor().withAlpha(0.5f).colorInt())
        native.setTypeface(
            TypefaceCompat.create(
                native.context,
                theme.font.font,
                theme.font.weight,
                theme.font.italic
            )
        )
        useAllCaps = theme.font.allCaps
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, theme.font.size.value.toFloat())
    }

    actual val content: ImmediateWritable<String> = native.contentProperty()
    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return t
    }

    private var useSensitiveDotMask = false
        set(value) {
            field = value
            updateTransformationMethod()
        }
    private var useAllCaps = false
        set(value) {
            field = value
            updateTransformationMethod()
        }
    private fun updateTransformationMethod() {
        // Calling EditText.setAllCaps() calls EditText.setTransformationMethod() under the hood which leads to side
        // effects when we are using the transformationMethod for password masking; ONLY enforce all caps when we are
        // not masking passwords as password masking takes priority
        if (useSensitiveDotMask) {
            native.transformationMethod = PasswordTransformationMethod.getInstance()
        } else {
            native.isAllCaps = theme.font.allCaps
        }
    }

    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
            useSensitiveDotMask = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        native.setImeActionLabel(value?.title, KeyEvent.KEYCODE_ENTER)
        native.setOnEditorActionListener { v, actionId, event ->
            value?.startAction(this)
            value != null
        }
    }

    actual var hint: String
        get() {
            return native.hint.toString()
        }
        set(value) {
            native.hint = value
        }
    actual var align: Align
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
                Align.Start -> native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                Align.End -> native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
                Align.Center -> native.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                Align.Stretch -> {
                    native.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                    native.updateLayoutParams<ViewGroup.LayoutParams> {
                        this.width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        }

    init {
        keyboardHints = KeyboardHints(KeyboardCase.Sentences)
    }
}


abstract class EquatableByRef(val key: String, val ref: Any) {
    override fun hashCode(): Int = key.hashCode() + ref.hashCode()
    override fun equals(other: Any?): Boolean = other is EquatableByRef && this.key == other.key && this.ref == other.ref
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
            it or (this.inputType and (InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE))
        }
        n.inputType = inputType
        when (value.autocomplete) {
            AutoComplete.Email -> View.AUTOFILL_HINT_EMAIL_ADDRESS
            AutoComplete.Password, AutoComplete.NewPassword -> View.AUTOFILL_HINT_PASSWORD
            AutoComplete.Phone -> View.AUTOFILL_HINT_PHONE
            null -> null
        }?.let { n.setAutofillHints(it) }
    }
