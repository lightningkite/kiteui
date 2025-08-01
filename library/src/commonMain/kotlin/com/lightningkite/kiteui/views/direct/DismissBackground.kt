package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class DismissBackground(context: RContext) : RView {
    public fun onClick(action: suspend () -> Unit)
}