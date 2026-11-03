package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
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
import com.lightningkite.kiteui.utils.repairFormatAndPosition
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

    private var isRawData: (Char) -> Boolean = { true }
    private var formatter: (clean: String) -> String = { it }

    public actual fun format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
        this.isRawData = isRawData
        this.formatter = formatter
    }

    /** The raw data only, without the formatting characters. */
    public actual val content: MutableReactiveValue<String> = Signal("").lens(get = { it }, set = { it.filter(isRawData) })
    public actual var hint: String by mutableStateOf("")
    public actual var align: Align? by mutableStateOf(null)
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints(KeyboardCase.Sentences))

    private var field by mutableStateOf(TextFieldValue(""))

    init {
        reactive {
            val raw = content()
            if (raw != field.text.filter(isRawData)) field = formatter(raw).let { TextFieldValue(it, TextRange(it.length)) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val hints = keyboardHints
        val style = theme.font.toTextStyle(color.toCompose(), align ?: theme.font.align)
        BasicTextField(
            value = field,
            onValueChange = {
                // Selection-only changes must not be reformatted, or the cursor can't rest beside a formatting character.
                field = if (it.text == field.text) it else it.reformatted(isRawData, formatter)
                content.value = field.text.filter(isRawData)
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
                Box {
                    if (field.text.isEmpty()) BasicText(theme.font.cased(hint), style = style.copy(color = theme.hintColor))
                    inner()
                }
            },
        )
    }
}

/** Reapplies a format after an edit, keeping the cursor on the same data character. */
internal fun TextFieldValue.reformatted(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String): TextFieldValue {
    var result = this
    repairFormatAndPosition(
        dirty = text,
        selectionStart = selection.start,
        selectionEnd = selection.end,
        setResult = { result = TextFieldValue(it) },
        setSelectionRange = { start, end -> result = result.copy(selection = TextRange(start, end)) },
        isRawData = isRawData,
        formatter = formatter,
    )
    return result
}
