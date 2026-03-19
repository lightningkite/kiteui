package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.*


expect class RowOrColOld(context: ElementContext) : RView {
    var vertical: Boolean
    fun spacingOverrideBeforeNext(amount: Dimension)
}

expect class RowWrappingOld(context: ElementContext) : RView

expect class RowCollapsingToColumnOld(context: ElementContext, breakpoints: List<Dimension>) : RView
