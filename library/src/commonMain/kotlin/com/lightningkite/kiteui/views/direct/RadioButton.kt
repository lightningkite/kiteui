package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.contracts.*
import kotlin.jvm.JvmInline


expect class RadioButton(context: RContext) : RView {

    var enabled: Boolean
    val checked: Writable<Boolean>
}