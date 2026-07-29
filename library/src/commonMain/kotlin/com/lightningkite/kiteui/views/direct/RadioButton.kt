package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.InteractiveElement
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.*


public expect class RadioButton(context: ElementContext) : InteractiveElement, NativeElement {
    override val underlyingNativeElement: RadioButton

    public val checked: MutableReactiveValue<Boolean>
    override var enabled: Boolean
}