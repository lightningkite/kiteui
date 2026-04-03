package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.InteractiveElement
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.*

expect class Checkbox(context: ElementContext) : InteractiveElement, NativeElement {
    override val underlyingNativeElement: Checkbox
    override var enabled: Boolean
    val checked: MutableReactiveValue<Boolean>
}