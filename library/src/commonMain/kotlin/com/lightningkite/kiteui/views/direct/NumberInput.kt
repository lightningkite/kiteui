package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction

typealias NumberField = NumberInput
expect class NumberInput(context: RContext) : RViewWithAction {

    var enabled: Boolean
    val content: ImmediateWritable<Double?>
    var keyboardHints: KeyboardHints
    var hint: String
    var range: ClosedRange<Double>?
    var align: Align
}