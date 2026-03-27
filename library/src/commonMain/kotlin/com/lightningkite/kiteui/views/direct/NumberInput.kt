package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

expect class NumberInput(context: ElementContext) : NativeElementWithAction {
    val content: MutableReactiveValue<Double?>
    var keyboardHints: KeyboardHints
    var hint: String
    var range: ClosedRange<Double>?
    var align: Align?
}