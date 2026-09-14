package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import com.lightningkite.reactive.core.*

public expect class RadioToggleButton(context: ElementContext) : NativeInteractiveContainerElement {
    public val checked: MutableReactiveValue<Boolean>
}