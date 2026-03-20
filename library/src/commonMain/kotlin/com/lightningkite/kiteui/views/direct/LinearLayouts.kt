package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement

interface LinearLayoutElement : ContainerElement {
    var gap: Dimension?
}

expect class RowOrCol(context: ElementContext) : NativeContainerElement, LinearLayoutElement {
    var vertical: Boolean
    override var gap: Dimension?

    @Deprecated("This no longer works.")
    fun spacingOverrideBeforeNext(amount: Dimension) // TODO: Find alternative, will need new syntax
}

expect class RowWrapping(context: ElementContext) : NativeContainerElement, LinearLayoutElement {
    override var gap: Dimension?
}

expect class RowCollapsingToColumn(context: ElementContext, breakpoints: List<Dimension>) : NativeContainerElement, LinearLayoutElement {
    override var gap: Dimension?
}