package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*


expect class Switch(context: ElementContext) : NativeElement {

    var enabled: Boolean
    val checked: MutableReactiveValue<Boolean>
}