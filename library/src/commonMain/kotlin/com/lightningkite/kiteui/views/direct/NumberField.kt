package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class NumberField(context: RContext) : RView {

    var enabled: Boolean
    val content: ImmediateWritable<Double?>
    var keyboardHints: KeyboardHints
    var action: Action?
    var hint: String
    var range: ClosedRange<Double>?
    var align: Align
    var textSize: Dimension
}