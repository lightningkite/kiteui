package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView


expect class ProgrammaticLayout(context: RContext): RView {
    fun measureChild(child: RView, sizeConstraint: Size): Size
    fun setChildBounds(child: RView, rect: Rect)
    val externalSizeLimit: Readable<Size>
}
