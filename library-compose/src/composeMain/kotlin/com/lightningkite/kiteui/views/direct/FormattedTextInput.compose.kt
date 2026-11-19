package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.reformatWithSelection
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.kiteui.views.hintColor
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle
import com.lightningkite.kiteui.views.cased
import com.lightningkite.kiteui.views.casing
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

public actual class FormattedTextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = formattedTextInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + formattedTextInputDriverActions()

    public actual var format: TextFormat = TextFormat.None
        set(value) {
            val data = content.value
            field = value
            content.value = data
            // Claude: the data can be unchanged while its layout isn't, so the shown text must be redone here.
            fieldValue = format.format(content.value).let { TextFieldValue(it, TextRange(it.length)) }
        }

    // Claude: stores the data of the formatted text, as the other platforms read it back from the field, so a value
    // set by code that the format would change can't disagree with what is shown.
    public actual val content: MutableReactiveValue<String> =
        Signal("").lens(get = { it }, set = { format.format(it.filter(format::isRawData)).filter(format::isRawData) })
    public actual var hint: String by mutableStateOf("")
    public actual var align: Align? by mutableStateOf(null)
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints(KeyboardCase.Sentences))

    private var fieldValue by mutableStateOf(TextFieldValue(""))

    init {
        reactive {
            val raw = content()
            if (raw != fieldValue.text.filter(format::isRawData)) fieldValue = format.format(raw).let { TextFieldValue(it, TextRange(it.length)) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val hints = keyboardHints
        val style = theme.font.toTextStyle(color.toCompose(), align ?: theme.font.align)
        BasicTextField(
            value = fieldValue,
            onValueChange = {
                // Selection-only changes must not be reformatted, or the cursor can't rest beside a formatting character.
                fieldValue = if (it.text == fieldValue.text) it else it.reformatted(format)
                content.value = fieldValue.text.filter(format::isRawData)
            },
            modifier = modifier,
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            visualTransformation = if (hints.isPassword) PasswordVisualTransformation() else theme.font.casing,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@FormattedTextInput) }),
            decorationBox = { inner ->
                SingleLineDecoration(theme.font) {
                    if (fieldValue.text.isEmpty()) BasicText(theme.font.cased(hint), style = style.copy(color = theme.hintColor))
                    inner()
                }
            },
        )
    }
}

/** Reapplies a format after an edit, keeping the cursor on the same data character. */
internal fun TextFieldValue.reformatted(format: TextFormat): TextFieldValue {
    val result = format.reformatWithSelection(text, selection.min until selection.max)
    return TextFieldValue(result.text, TextRange(result.selection.first, result.selection.last + 1))
}
