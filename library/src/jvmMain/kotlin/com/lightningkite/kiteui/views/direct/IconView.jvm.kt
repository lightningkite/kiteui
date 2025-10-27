package com.lightningkite.kiteui.views.direct

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Icon as KiteIcon
import com.lightningkite.kiteui.models.toPainter
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class IconView actual constructor(context: RContext) :
    RView(context) {
    private val m_source = Signal<KiteIcon?>(null)
    actual var source: KiteIcon? by m_source

    private val m_description = Signal<String?>(null)
    actual var description: String? by m_description

    @Composable
    override fun compose() {
        val source = m_source.value
        val description = m_description.value

        source?.let {
            val painter = it.toImageSource(Color.red).toPainter()
            Icon(
                painter = painter,
                contentDescription = description,
                modifier = Modifier
            )
        }
    }
}