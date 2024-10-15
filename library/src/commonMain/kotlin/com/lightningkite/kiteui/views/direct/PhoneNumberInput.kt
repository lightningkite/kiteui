package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction

expect class PhoneNumberInput(context: RContext) : RViewWithAction {
    var enabled: Boolean
    val content: ImmediateWritable<String>
    var hint: String
    var align: Align
    var textSize: Dimension
}