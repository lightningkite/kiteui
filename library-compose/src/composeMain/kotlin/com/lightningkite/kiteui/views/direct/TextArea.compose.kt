package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
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

public actual class TextArea actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = textAreaDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + textAreaDriverActions()

    public actual val content: MutableReactiveValue<String> = Signal("")
    public actual var keyboardHints: KeyboardHints by mutableStateOf(KeyboardHints())
    public actual var hint: String by mutableStateOf("")

    private var text by mutableStateOf("")

    init {
        reactive { text = content() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val color = theme.foreground.closestColor()
        val style = theme.font.toTextStyle(color.toCompose())
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                content.value = it
            },
            // As on the web: with an action, Enter submits and Shift+Enter makes a new line.
            modifier = modifier.onPreviewKeyEvent {
                val action = action
                if (action == null || it.key != Key.Enter || it.type != KeyEventType.KeyDown || it.isShiftPressed) return@onPreviewKeyEvent false
                action.startAction(this@TextArea)
                true
            },
            enabled = enabled && takesInput,
            textStyle = style,
            cursorBrush = SolidColor(color.toCompose()),
            interactionSource = interactions.source,
            keyboardOptions = keyboardHints.toKeyboardOptions(imeAction = ImeAction.Default),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) BasicText(hint, style = style.copy(color = color.withAlpha(0.5f).toCompose()))
                    field()
                }
            },
        )
    }
}
