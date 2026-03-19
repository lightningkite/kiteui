package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*


expect class Checkbox(context: ElementContext) : NativeElement {

    var enabled: Boolean
    val checked: MutableReactiveValue<Boolean>
}