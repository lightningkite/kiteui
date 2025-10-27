package com.lightningkite.kiteui.views.direct

import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import kotlinx.coroutines.launch

actual class FormattedTextInput actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private val m_content = Signal("")
    actual val content: MutableReactiveValue<String> = m_content

    private val m_hint = Signal("")
    actual var hint: String by m_hint

    private val m_align = Signal(Align.Start)
    actual var align: Align by m_align

    private val m_keyboardHints = Signal(KeyboardHints())
    actual var keyboardHints: KeyboardHints by m_keyboardHints

    private var isRawDataFn: ((Char) -> Boolean)? = null
    private var formatterFn: ((String) -> String)? = null

    actual fun format(isRawData: (Char) -> Boolean, formatter: (clean: String) -> String) {
        this.isRawDataFn = isRawData
        this.formatterFn = formatter
    }

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val content = m_content.collectAsMutableState()
        val hint = m_hint.collectAsMutableState()
        val align = m_align.collectAsMutableState()
        val scope = rememberCoroutineScope()

        val textAlign = when (align.value) {
            Align.Start -> TextAlign.Start
            Align.Center -> TextAlign.Center
            Align.End -> TextAlign.End
            else -> TextAlign.Start
        }

        OutlinedTextField(
            value = content.value,
            onValueChange = { newValue ->
                // Apply formatting if configured
                val formattedValue = if (isRawDataFn != null && formatterFn != null) {
                    val cleanData = newValue.filter { isRawDataFn!!(it) }
                    formatterFn!!(cleanData)
                } else {
                    newValue
                }

                m_content.value = formattedValue

                scope.launch {
                    action?.invoke()
                }
            },
            enabled = enabled.value,
            label = { Text(hint.value) },
            textStyle = androidx.compose.ui.text.TextStyle(textAlign = textAlign)
        )
    }
}