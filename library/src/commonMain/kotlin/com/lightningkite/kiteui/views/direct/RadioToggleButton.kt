package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class RadioToggleButton(context: RContext) : RView {

    var enabled: Boolean
    val checked: ImmediateWritable<Boolean>
}