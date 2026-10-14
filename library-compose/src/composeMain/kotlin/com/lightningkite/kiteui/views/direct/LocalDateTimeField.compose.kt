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
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.datetime.LocalDateTime

// Typed as yyyy-mm-dd hh:mm (24-hour); like the web's datetime-local input, content stays null until the text is a
// whole, valid date and time.
// TODO: Compose backend: a date and time picker popover limited to range, once openPopover exists.
public actual class LocalDateTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localDateTimeDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localDateTimeDriverActions()

    public actual val content: MutableReactiveValue<LocalDateTime?> = Signal(null)
    // As on the web, typed values aren't held to the range.
    public actual var range: ClosedRange<LocalDateTime>? = null

    private var field by mutableStateOf(TextFieldValue(""))

    private fun parse(text: String): LocalDateTime? =
        if (text.length == 16) try { LocalDateTime.parse(text.replace(' ', 'T')) } catch (e: IllegalArgumentException) { null } else null

    init {
        reactive {
            val dateTime = content()
            // Partly typed text parses to null too; leave it alone while that null comes back around.
            // Seconds are dropped, as the web's datetime-local input does.
            if (dateTime != parse(field.text)) field = (dateTime?.toString()?.take(16)?.replace('T', ' ') ?: "").let { TextFieldValue(it, TextRange(it.length)) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        interactions.Track()
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val style = theme.font.toTextStyle(color.toCompose())
        BasicTextField(
            value = field,
            onValueChange = { typed ->
                field = if (typed.text == field.text) typed else typed.reformatted(Char::isDigit) { digits ->
                    buildString {
                        digits.take(12).forEachIndexed { i, digit ->
                            when (i) {
                                4, 6 -> append('-')
                                8 -> append(' ')
                                10 -> append(':')
                            }
                            append(digit)
                        }
                    }
                }
                content.value = parse(field.text)
            },
            modifier = modifier,
            enabled = enabled && !ignoreInteraction,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@LocalDateTimeField) }),
            decorationBox = { inner ->
                Box {
                    if (field.text.isEmpty()) BasicText("yyyy-mm-dd hh:mm", style = style.copy(color = color.withAlpha(0.5f).toCompose()))
                    inner()
                }
            },
        )
    }
}
