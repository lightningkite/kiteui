package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public typealias NumberField = NumberInput
public expect class NumberInput(context: RContext) : RViewWithAction {

    public var enabled: Boolean
    public val content: MutableReactiveValue<Double?>
    public var keyboardHints: KeyboardHints
    public var hint: String
    public var range: ClosedRange<Double>?
    public var align: Align
}