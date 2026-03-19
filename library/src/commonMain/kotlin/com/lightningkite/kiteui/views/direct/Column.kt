package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement


expect class RowOrCol(context: ElementContext) : NativeContainerElement {
    var vertical: Boolean

    @Deprecated("This no longer works.")
    fun spacingOverrideBeforeNext(amount: Dimension) // TODO: Find alternative, will need new syntax
}

expect class RowWrapping(context: ElementContext) : NativeContainerElement

expect class RowCollapsingToColumn(context: ElementContext, breakpoints: List<Dimension>) : NativeContainerElement