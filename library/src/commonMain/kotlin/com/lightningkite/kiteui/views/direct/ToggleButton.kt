package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*


expect class ToggleButton(context: ElementContext) : RView {

    var enabled: Boolean
    val checked: MutableReactiveValue<Boolean>
}