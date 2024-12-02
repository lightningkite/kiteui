package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction

expect class FormattedTextInput(context: RContext) : RViewWithAction {
    var enabled: Boolean
    val content: ImmediateWritable<String>
    var hint: String
    var align: Align
    var keyboardHints: KeyboardHints

    fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    )
}