package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.ResizeObserver
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import org.w3c.dom.*

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

actual fun Canvas.setupResizeListener() {
    native.onElement { htmlNative ->
        htmlNative as HTMLCanvasElement
        ResizeObserver { _, _ ->
            htmlNative.apply {
                if (width != scrollWidth || height != scrollHeight) {
                    width = scrollWidth
                    height = scrollHeight
                }
                delegate?.onResize(scrollWidth.toDouble(), scrollHeight.toDouble())
                delegate?.invalidate?.invoke()
            }

        }.observe(htmlNative)
    }
}