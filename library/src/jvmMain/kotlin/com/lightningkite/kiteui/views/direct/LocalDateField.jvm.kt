package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.DateTimeFormatter

actual class LocalDateField actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val m_content = Signal<LocalDate?>(null)
    actual val content: MutableReactiveValue<LocalDate?> = m_content

    private var m_range: ClosedRange<LocalDate>? = null
    actual var range: ClosedRange<LocalDate>?
        get() = m_range
        set(value) {
            m_range = value
        }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Composable
    override fun compose() {
        val contentState = m_content.collectAsMutableState()
        val scope = rememberCoroutineScope()

        val displayText = contentState.value?.let {
            it.toJavaLocalDate().format(dateFormatter)
        } ?: ""

        OutlinedTextField(
            value = displayText,
            onValueChange = { newValue ->
                try {
                    val parsed = java.time.LocalDate.parse(newValue, dateFormatter).toKotlinLocalDate()
                    if (m_range?.contains(parsed) != false) {
                        m_content.value = parsed
                        scope.launch {
                            action?.invoke()
                        }
                    }
                } catch (e: Exception) {
                    // Invalid date format, ignore
                }
            },
            modifier = Modifier.clickable {
                // TODO: Show date picker dialog
            },
            label = { androidx.compose.material.Text("Date (yyyy-MM-dd)") },
            readOnly = false
        )
    }
}