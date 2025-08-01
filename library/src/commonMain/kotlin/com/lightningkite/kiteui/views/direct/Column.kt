package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.*


public expect class RowOrCol(context: RContext) : RView {
    public var vertical: Boolean
    public fun spacingOverrideBeforeNext(amount: Dimension)
}
expect class RowWrapping(context: RContext) : RView {
}