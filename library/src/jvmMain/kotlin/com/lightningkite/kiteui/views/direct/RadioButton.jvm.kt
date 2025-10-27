package com.lightningkite.kiteui.views.direct

import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class RadioButton actual constructor(context: RContext) :
    RView(context) {
    val m_enabled = Signal<Boolean>(true)
    actual var enabled: Boolean by m_enabled

    actual val checked: MutableReactiveValue<Boolean> = Signal<Boolean>(false)

    @Composable
    override fun compose() {
        val isChecked = checked.collectAsMutableState()
        val isEnabled = m_enabled.collectAsMutableState()
        RadioButton(
            selected = isChecked.value,
            onClick = {
                checked.value = true
            },
            enabled = isEnabled.value
        )
    }
}