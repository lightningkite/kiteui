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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

public actual class AutoCompleteTextField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + autoCompleteDriverActions()

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints())
    public actual var hint: String by mutableStateOf("")
    public actual var suggestions: List<String> by mutableStateOf(emptyList())

    private var text by mutableStateOf("")
    // Open while the user is typing; closed by choosing, dismissing or leaving the field.
    private var suggesting by mutableStateOf(false)

    init {
        reactive { text = content() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        interactions.Track()
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val hints = keyboardHints
        val style = theme.font.toTextStyle(color.toCompose())
        // Like a browser's datalist: suggestions containing the text, ignoring case.
        val matches = suggestions.filter { it.contains(text, ignoreCase = true) && it != text }
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                content.value = it
                suggesting = true
            },
            modifier = modifier.onFocusChanged { if (!it.isFocused) suggesting = false },
            enabled = enabled && !ignoreInteraction,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            visualTransformation = if (hints.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@AutoCompleteTextField) }),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) BasicText(hint, style = style.copy(color = color.withAlpha(0.5f).toCompose()))
                    field()
                    if (suggesting && matches.isNotEmpty()) DropdownList(
                        owner = this@AutoCompleteTextField,
                        options = matches,
                        focusable = false,
                        onChoose = {
                            text = matches[it]
                            content.value = matches[it]
                            suggesting = false
                        },
                        onDismiss = { suggesting = false },
                    )
                }
            },
        )
    }
}
