package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.contracts.*
import kotlin.jvm.JvmInline


expect class RadioButton(context: RContext) : RView {
    var enabled: Boolean
    val checked: ImmediateWritable<Boolean>
}