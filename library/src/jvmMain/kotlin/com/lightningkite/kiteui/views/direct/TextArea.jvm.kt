package com.lightningkite.kiteui.views.direct

import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

actual class TextArea actual constructor(context: RContext) :
    RViewWithAction(context) {

    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    actual val content: MutableReactiveValue<String> = Signal("")
    private val m_keyboardHints = Signal(KeyboardHints())
    actual var keyboardHints: KeyboardHints by m_keyboardHints

    private val m_hint = Signal("")
    actual var hint: String by m_hint

    @Composable
    override fun compose() {
        val value = content.collectAsMutableState()
        val hint = m_hint.collectAsMutableState()
        val enabled = m_enabled.collectAsMutableState()
        TextField(
            value = value.value,
            onValueChange = {
                value.value = it
            },
            label = { Text(hint.value) },
            enabled = enabled.value,
            maxLines = Int.MAX_VALUE
        )
    }
}