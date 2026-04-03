package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.InteractiveElement
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.*


expect class RadioButton(context: ElementContext) : InteractiveElement, NativeElement {
    override val underlyingNativeElement: RadioButton

    val checked: MutableReactiveValue<Boolean>
    override var enabled: Boolean
}