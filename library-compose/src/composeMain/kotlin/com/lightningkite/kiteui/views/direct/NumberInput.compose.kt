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
import androidx.compose.ui.text.input.TextFieldValue
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
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

public actual class NumberInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = numberInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + numberInputDriverActions()

    public actual val content: MutableReactiveValue<Double?> = Signal(null)
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints.decimal)
    public actual var hint: String by mutableStateOf("")
    // As on the web, typed values aren't held to the range; only the AI driver's setValue clamps to it.
    public actual var range: ClosedRange<Double>? = null
    public actual var align: Align? by mutableStateOf(null)

    private var field by mutableStateOf(TextFieldValue(""))

    private fun parse(text: String): Double? = text.filter { it.isDigit() || it == '-' || it == '.' }.toDoubleOrNull()

    init {
        reactive {
            val number = content()
            // Text like "-" or "" parses to null too; leave it alone while that null comes back around.
            if (number != parse(field.text)) field = (number?.commaString() ?: "").let { TextFieldValue(it, TextRange(it.length)) }
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
            onValueChange = { typed ->
                if (typed.text == field.text) {
                    field = typed
                    return@BasicTextField
                }
                numberAutocommaRepair(
                    dirty = typed.text,
                    selectionStart = typed.selection.start,
                    selectionEnd = typed.selection.end,
                    allowDecimal = hints.type.allowDecimal,
                    setResult = { field = TextFieldValue(it) },
                    setSelectionRange = { start, end -> field = field.copy(selection = TextRange(start, end)) },
                )
                content.value = parse(field.text)
            },
            modifier = modifier,
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            visualTransformation = theme.font.casing,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@NumberInput) }),
            decorationBox = { inner ->
                Box {
                    if (field.text.isEmpty()) BasicText(theme.font.cased(hint), style = style.copy(color = theme.hintColor))
                    inner()
                }
            },
        )
    }
}
