package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.kiteui.views.hintColor
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.datetime.LocalTime

// Typed as 24-hour hh:mm; like the web's time input, content stays null until the text is a whole, valid time.
// TODO: Compose backend: a picker.  The web's native input opens scrolling hour and minute columns from a clock
//  icon at the field's end, with times outside range disabled; build it on openPopover.
public actual class LocalTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localTimeDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localTimeDriverActions()

    public actual val content: MutableReactiveValue<LocalTime?> = Signal(null)
    // As on the web, typed times aren't held to the range.
    public actual var range: ClosedRange<LocalTime>? = null

    private var field by mutableStateOf(TextFieldValue(""))

    private fun parse(text: String): LocalTime? =
        if (text.length == 5) try { LocalTime.parse(text) } catch (e: IllegalArgumentException) { null } else null

    init {
        reactive {
            val time = content()
            // Partly typed text parses to null too; leave it alone while that null comes back around.
            // Seconds are dropped, as the web's time input does.
            if (time != parse(field.text)) field = (time?.toString()?.take(5) ?: "").let { TextFieldValue(it, TextRange(it.length)) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val style = theme.font.toTextStyle(color.toCompose())
        BasicTextField(
            value = field,
            onValueChange = { typed ->
                field = if (typed.text == field.text) typed else typed.reformatted(Char::isDigit) { digits ->
                    buildString {
                        digits.take(4).forEachIndexed { i, digit ->
                            if (i == 2) append(':')
                            append(digit)
                        }
                    }
                }
                content.value = parse(field.text)
            },
            modifier = modifier,
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@LocalTimeField) }),
            decorationBox = { inner ->
                Box {
                    if (field.text.isEmpty()) BasicText("hh:mm", style = style.copy(color = theme.hintColor))
                    inner()
                }
            },
        )
    }
}
