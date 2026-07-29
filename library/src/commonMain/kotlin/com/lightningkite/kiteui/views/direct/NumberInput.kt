package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

public expect class NumberInput(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<Double?>
    public var keyboardHints: KeyboardHints
    public var hint: String
    public var range: ClosedRange<Double>?
    public var align: Align?
}