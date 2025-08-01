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

typealias NumberField = NumberInput
expect class NumberInput(context: RContext) : RViewWithAction {

    var enabled: Boolean
    val content: MutableReactiveValue<Double?>
    var keyboardHints: KeyboardHints
    var hint: String
    var range: ClosedRange<Double>?
    var align: Align
}