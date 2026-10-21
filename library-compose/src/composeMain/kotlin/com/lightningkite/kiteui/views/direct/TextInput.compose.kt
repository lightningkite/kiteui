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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.lightningkite.kiteui.models.*
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
import androidx.compose.ui.text.input.KeyboardType as ComposeKeyboardType

public actual class TextInput actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = textInputDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + textInputDriverActions()

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints(KeyboardCase.Sentences))
    public actual var hint: String by mutableStateOf("")
    public actual var align: Align? by mutableStateOf(null)

    private var text by mutableStateOf("")

    init {
        reactive { text = content() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val hints = keyboardHints
        val style = theme.font.toTextStyle(color.toCompose(), align ?: theme.font.align)
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                content.value = it
            },
            modifier = modifier,
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            visualTransformation = if (hints.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@TextInput) }),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) BasicText(hint, style = style.copy(color = theme.hintColor))
                    field()
                }
            },
        )
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
