package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.padding

public interface LinearLayoutElement : ContainerElement {
    public var gap: Dimension?

    @Deprecated("Will probably be removed in the future")
    override val spacingForChildCornerRadii: Dimension get() {
        val pad = padding ?: themeAndBack.theme.padding.top
        val gap = gap ?: themeAndBack.theme.gap
        return minOf(pad, gap)
    }
}

public expect class RowOrCol(context: ElementContext) : NativeContainerElement, LinearLayoutElement {
    public var vertical: Boolean
    override var gap: Dimension?

    // Intentionally retained despite being ERROR-deprecated: this is a wanted capability pending a
    // syntax redesign, not dead code to remove (maintainer decision).
    @Deprecated("This no longer works.", level = DeprecationLevel.ERROR)
    public fun spacingOverrideBeforeNext(amount: Dimension) // TODO: Find alternative, will need new syntax
}

public expect class RowWrapping(context: ElementContext) : NativeContainerElement, LinearLayoutElement {
    override var gap: Dimension?
}

public expect class RowCollapsingToColumn(context: ElementContext, breakpoints: List<Dimension>) : NativeContainerElement, LinearLayoutElement {
    override var gap: Dimension?
}