package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.*

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    actual fun measureChild(
        child: RView,
        sizeConstraint: Size
    ): Size = Size(0.0, 0.0)


    actual fun setChildBounds(child: RView, rect: Rect) {
        child.native.style.position = "absolute"
        child.native.style.width = rect.width.toString() + "px"
        child.native.style.height = rect.width.toString() + "px"
        child.native.style.left = rect.left.toString() + "px"
        child.native.style.top = rect.top.toString() + "px"
    }

    actual val externalSizeLimit: Readable<Size> = Constant(Size(0.0, 0.0))
}