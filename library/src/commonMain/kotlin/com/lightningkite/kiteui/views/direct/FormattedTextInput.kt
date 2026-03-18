package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*

expect class FormattedTextInput(context: ElementContext) : RViewWithAction {
    var enabled: Boolean
    val content: MutableReactiveValue<String>
    var hint: String
    var align: Align?
    var keyboardHints: KeyboardHints

    fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    )
}