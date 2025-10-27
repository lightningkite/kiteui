package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class RadioToggleButton actual constructor(context: RContext) :
    RView(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private val m_checked = Signal(false)
    actual val checked: MutableReactiveValue<Boolean> = m_checked

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val checked = m_checked.collectAsMutableState()

        Button(
            onClick = {
                if (enabled.value && !checked.value) {
                    // Radio buttons typically only toggle on (not off)
                    m_checked.value = true
                }
            },
            enabled = enabled.value,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (checked.value) Color.Blue else Color.Gray,
                contentColor = Color.White,
                disabledBackgroundColor = Color.LightGray,
                disabledContentColor = Color.DarkGray
            )
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                children.forEach {
                    it.compose()
                }
            }
        }
    }
}