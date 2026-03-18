package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.*


expect class RowOrCol(context: ElementContext) : RView {
    var vertical: Boolean
    fun spacingOverrideBeforeNext(amount: Dimension)
}
expect class RowWrapping(context: ElementContext) : RView {
}