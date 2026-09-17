package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.*


public expect class Switch(context: ElementContext) : NativeInteractiveElement {
    public val checked: MutableReactiveValue<Boolean>
}