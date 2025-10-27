package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2D

actual class Canvas actual constructor(context: RContext) :
    RView(context) {
    private val m_delegate = Signal<CanvasDelegate?>(null)
    actual var delegate: CanvasDelegate? by m_delegate

    @Composable
    override fun compose() {
        val delegate = m_delegate.value
        ComposeCanvas(modifier = Modifier) {
            // TODO: Implement DrawingContext2D properly
            // delegate?.draw(DrawingContext2DImpl(this))
        }
    }
}