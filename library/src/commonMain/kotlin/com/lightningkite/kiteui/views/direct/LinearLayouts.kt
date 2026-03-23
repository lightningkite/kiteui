package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.padding

interface LinearLayoutElement : ContainerElement {
    var gap: Dimension?

    @Deprecated("Will probably be removed in the future")
    override val spacingForChildCornerRadii: Dimension get() {
        val pad = padding ?: themeAndBack.theme.padding.top
        val gap = gap ?: themeAndBack.theme.gap
        return minOf(pad, gap)
    }
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