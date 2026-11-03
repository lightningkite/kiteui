package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*

public expect class FormattedTextInput(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<String>
    public var hint: String
    public var align: Align?
    public var keyboardHints: KeyboardHints

    public fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    )
}