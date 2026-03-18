package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.RView


expect class DismissBackground(context: ElementContext) : RView {
    fun onClick(action: suspend () -> Unit)
}