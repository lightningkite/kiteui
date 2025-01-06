package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.lens
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLElement

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }

    actual fun measureChild(
        child: RView,
        sizeConstraint: Size
    ): Size = (child.native.element as? HTMLElement)?.let {
        val tempchildwidth = child.native.style.width
        child.native.style.width = "unset"
        val tempchildheight = child.native.style.height
        child.native.style.height = "unset"
        val out = Size(it.scrollWidth.toDouble() + 1.0, it.scrollHeight.toDouble() + 1.0)
        child.native.style.width = tempchildwidth
        child.native.style.height = tempchildheight
        out
    } ?: Size(0.0, 0.0)

    actual fun setChildBounds(child: RView, rect: Rect) {
        child.native.style.position = "absolute"
        child.native.style.width = rect.width.toString() + "px"
        child.native.style.height = rect.height.toString() + "px"
        child.native.style.left = rect.left.toString() + "px"
        child.native.style.top = rect.top.toString() + "px"
    }

    actual val externalSizeLimit: Readable<Size> = native.resizeObserver().lens {
        Size(native.element?.clientWidth?.toDouble() ?: 0.0, native.element?.clientHeight?.toDouble() ?: 0.0)
    }
}