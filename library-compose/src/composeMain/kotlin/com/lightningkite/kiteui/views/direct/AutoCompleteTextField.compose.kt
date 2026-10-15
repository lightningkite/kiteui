package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import com.lightningkite.kiteui.models.KeyboardHints
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

public actual class AutoCompleteTextField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + autoCompleteDriverActions()

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints())
    public actual var hint: String by mutableStateOf("")
    public actual var suggestions: List<String> by mutableStateOf(emptyList())

    private var field by mutableStateOf(TextFieldValue(""))
    // Open while the user is typing; closed by choosing, dismissing or leaving the field.
    private var suggesting by mutableStateOf(false)

    init {
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
        val style = theme.font.toTextStyle(color.toCompose())
        // Like a browser's datalist: suggestions containing the text, ignoring case.
        val matches = suggestions.filter { it.contains(field.text, ignoreCase = true) && it != field.text }
        val showsList = suggesting && matches.isNotEmpty()
        val list = remember(matches) {
            DropdownList(
                options = matches,
                highlighted = -1,
                onChoose = {
                    val choice = matches[it]
                    field = TextFieldValue(choice, TextRange(choice.length))
                    content.value = choice
                    suggesting = false
                },
                onDismiss = { suggesting = false },
            )
        }
        BasicTextField(
            value = field,
            onValueChange = {
                val edited = it.text != field.text
                field = it
                if (edited) {
                    content.value = it.text
                    suggesting = true
                }
            },
            modifier = modifier
                .onFocusChanged { if (!it.isFocused) suggesting = false }
                .onPreviewKeyEvent { showsList && list.onKeyEvent(it) },
            enabled = enabled && takesInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            visualTransformation = if (hints.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = hints.toKeyboardOptions(imeAction = if (action != null) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { action?.startAction(this@AutoCompleteTextField) }),
            decorationBox = { innerField ->
                Box {
                    if (field.text.isEmpty()) BasicText(hint, style = style.copy(color = theme.hintColor))
                    innerField()
                    if (showsList) list.Render(owner = this@AutoCompleteTextField, focusable = false)
                }
            },
        )
    }
}
