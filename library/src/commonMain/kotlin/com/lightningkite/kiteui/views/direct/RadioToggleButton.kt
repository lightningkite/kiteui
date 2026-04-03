package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import com.lightningkite.reactive.core.*

expect class RadioToggleButton(context: ElementContext) : NativeInteractiveContainerElement {
    val checked: MutableReactiveValue<Boolean>
}