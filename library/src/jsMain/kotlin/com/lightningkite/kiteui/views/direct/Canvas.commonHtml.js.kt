package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND

actual fun Canvas.onDelegateSet(delegate: CanvasDelegate?) {
    delegate?.let { value ->
        value.invalidate = {
            native.onElement {
                it as HTMLCanvasElement
                it.getContext("2d").apply {
                    this as DrawingContext2D
                    this.lineCap = CanvasLineCap.ROUND
                    this.lineJoin = CanvasLineJoin.ROUND
                    value.draw(this)
                }
            }
        }
        value.invalidate()
    }
}