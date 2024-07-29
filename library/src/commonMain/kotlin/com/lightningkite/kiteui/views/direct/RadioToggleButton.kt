package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class RadioToggleButton(context: RContext) : RView {

    var enabled: Boolean
    val checked: ImmediateWritable<Boolean>
}