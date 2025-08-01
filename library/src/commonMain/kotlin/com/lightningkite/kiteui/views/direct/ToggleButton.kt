package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class ToggleButton(context: RContext) : RView {

    public var enabled: Boolean
    public val checked: ImmediateWritable<Boolean>
}