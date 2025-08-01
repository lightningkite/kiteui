package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction

public typealias NumberField = NumberInput
public expect class NumberInput(context: RContext) : RViewWithAction {

    public var enabled: Boolean
    public val content: ImmediateWritable<Double?>
    public var keyboardHints: KeyboardHints
    public var hint: String
    public var range: ClosedRange<Double>?
    public var align: Align
}