package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.*


expect class RowOrCol(context: RContext, cannotBeCovered: Boolean = false) : RView {
    var vertical: Boolean
    fun spacingOverrideBeforeNext(amount: Dimension)
}