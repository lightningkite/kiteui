package com.lightningkite.kiteui.views.direct

import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class Slider actual constructor(context: RContext) :
    RView(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    actual val value: MutableReactiveValue<Float> = Signal(0f)

    private val m_min = Signal(0f)
    actual var min: Float by m_min

    private val m_max = Signal(1f)
    actual var max: Float by m_max

    private val m_step = Signal<Float?>(null)
    actual var step: Float? by m_step

    @Composable
    override fun compose() {
        val value = value.collectAsMutableState()
        val enabled = m_enabled.collectAsMutableState()
        val min = m_min.collectAsMutableState()
        val max = m_max.collectAsMutableState()
        val step = m_step.collectAsMutableState()

        Slider(
            value = value.value,
            onValueChange = {
                value.value = it
            },
            enabled = enabled.value,
            valueRange = min.value..max.value,
            steps = step.value?.let { ((max.value - min.value) / it).toInt() - 1 } ?: 0
        )
    }
}