package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.RView


expect class CircularProgress(context: ElementContext) : RView {
    var ratio: Float
}