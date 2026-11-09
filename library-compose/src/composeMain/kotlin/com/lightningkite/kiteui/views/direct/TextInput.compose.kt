package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
import androidx.compose.ui.text.input.KeyboardType as ComposeKeyboardType

public actual class TextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = textInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + textInputDriverActions()

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints(KeyboardCase.Sentences))
    public actual var hint: String by mutableStateOf("")
    public actual var align: Align? by mutableStateOf(null)

    private var field by mutableStateOf(TextFieldValue(""))

    init {
        // Claude: like setting an <input>'s value, a change from outside puts the cursor at the end.
        reactive {
            val text = content()
            if (text != field.text) field = TextFieldValue(text, TextRange(text.length))
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
                field = it
                content.value = it.text
            },
            modifier = modifier,
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            visualTransformation = if (hints.isPassword) PasswordVisualTransformation() else theme.font.casing,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@TextInput) }),
            decorationBox = { inner ->
                SingleLineDecoration(theme.font) {
                    if (field.text.isEmpty()) BasicText(theme.font.cased(hint), style = style.copy(color = theme.hintColor))
                    inner()
                }
            },
        )
    }
}

/**
 * Lays out a single-line field's text and placeholder ([content]) like an <input>: one line-height of [font] tall,
 * centered vertically when the field is taller, as when a row stretches it.
 */
@Composable
internal fun SingleLineDecoration(font: FontAndStyle, content: @Composable () -> Unit) {
    // Claude: the text field gives this its own size as minimum constraints, which the inner Box doesn't pass on.
    Box(contentAlignment = Alignment.CenterStart) {
        // Compose caps a single-line field at its line's height as Skia reports it, which for some fonts and sizes is
        // a pixel short of the line-height the text is laid out in.  The text keeps its place at the top.
        Box(Modifier.fillMaxWidth().heightIn(min = (font.size * font.lineSpacingMultiplier.toFloat()).toDp())) { content() }
    }
}

internal val KeyboardHints.isPassword: Boolean
    get() = autocomplete == AutoComplete.Password || autocomplete == AutoComplete.NewPassword

internal fun KeyboardHints.toKeyboardOptions(imeAction: ImeAction): KeyboardOptions = KeyboardOptions(
    capitalization = when (case) {
        KeyboardCase.None -> KeyboardCapitalization.None
        KeyboardCase.Letters -> KeyboardCapitalization.Characters
        KeyboardCase.Words -> KeyboardCapitalization.Words
        KeyboardCase.Sentences -> KeyboardCapitalization.Sentences
    },
    autoCorrectEnabled = autocorrect,
    keyboardType = when {
        isPassword -> ComposeKeyboardType.Password
        type == KeyboardType.Email -> ComposeKeyboardType.Email
        type == KeyboardType.Phone -> ComposeKeyboardType.Phone
        type == KeyboardType.Text -> ComposeKeyboardType.Text
        type.allowDecimal -> ComposeKeyboardType.Decimal
        else -> ComposeKeyboardType.Number
    },
    imeAction = imeAction,
)
