package com.lightningkite.kiteui.views.direct

import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class Checkbox actual constructor(context: RContext) :
    RView(context) {
    val m_enabled = Signal<Boolean>(true)
    actual var enabled: Boolean by m_enabled

    actual val checked: MutableReactiveValue<Boolean> = Signal<Boolean>(false)

    @Composable
    override fun compose() {
        val isChecked = checked.collectAsMutableState()
        val isEnabled = m_enabled.collectAsMutableState()
        Checkbox(
            checked = isChecked.value,
            onCheckedChange = {
                checked.value = it
            },
            enabled = isEnabled.value
        )
    }
}